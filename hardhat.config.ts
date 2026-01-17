import '@nomicfoundation/hardhat-toolbox';
import * as dotenv from 'dotenv';
import { HardhatUserConfig, task } from "hardhat/config";
import { getRawPublicKey } from './scripts/keys';
import { getContractAddress } from './scripts/addresses';
import setContractVersion from './scripts/set-contract-version';
import path from 'path';
import fs from 'fs';
import { GymVisitEventStruct, RunningEventStruct, SleepEventStruct } from './typechain-types/contracts/PhysicalActivityOracle';
import { signGymVisitEvent, signRunningEvent, signSleepEvent } from './test/helpers/Stats';
import { deployContract, FitnessUnbreakableVowUpkeeper, getContract, to96BytesString, installRip7212Mock } from './scripts/utils';
import verify from './scripts/verify-contract';
import { getTimeSettings, WeekDurations } from './scripts/timing';

// Defaults
const STAKED_AMOUNT = "0.0001";
const CREATION_DATE = new Date().toISOString();
const NUMBER_OF_CYLES = "2";
const SECONDS_IN_WEEK: WeekDurations = '3-minutes'; // 95min to run android test

dotenv.config({ path: '.env.local' });
dotenv.config({ path: '.env', override: true });

// Optional environment variables (may be missing in CI)
const SEPOLIA_RPC_URL = process.env['sepolia.RPC_URL'];
const ABI_SEP_RPC_URL = process.env['arbiSep.RPC_URL'];
const SEPOLIA_WALLET_PRIVATE_KEY = process.env['sepolia.WALLET_PRIVATE_KEY'];
const ARBITRUM_WALLET_PRIVATE_KEY = process.env['arbitrum.WALLET_PRIVATE_KEY'];
const ARBITRUM_SEPOLIA_WALLET_PRIVATE_KEY = process.env['arbiSep.WALLET_PRIVATE_KEY'];
const ETHERSCAN_API_KEY = process.env['ETHERSCAN_API_KEY'];
const COINMARKETCAP_API_KEY = process.env['GAS_REPORTER.COIN_MARKET_API_KEY'];

task('pre:compile')
    .setAction(async(_, { network: { name: networkName } }) => {
        const isLocalhost = networkName === 'localhost' || networkName === 'hardhat';
        const source = path.join(__dirname, 'contracts', 'lib', 'variants', isLocalhost ? 'hardhat' : 'standard');
        const destination = path.join(source, '..');

        for (const file of ['console.sol']) {
            fs.copyFileSync(path.join(source, file + '.source'), path.join(destination, file));
        }

        console.log(`🔁 Using ${isLocalhost ? 'hardhat' : 'standard'} source code variants for network ${networkName}`);
})

task("compile")
    .setAction(async (args, hre, runSuper) => {
        setContractVersion()
        hre.run('pre:compile')

        await runSuper(args);
    });

task('TerminateVow', "Terminates the FitnessUnbreakableVow.")
    .setAction(async (taskArgs, hre) => {
        const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));
        const contract = await getContract(hre, 'FitnessUnbreakableVow');

        console.log('Terminating contract.');
        const terminateTx = await contract.terminateAgreement(false);
        await terminateTx.wait();

        console.log('Withdrawing upkeeper link.');
        await sleep(15_000);

        const withdrawTx = await contract.terminateAgreement(true);
        await withdrawTx.wait();
    })

task('PushPhysicalActivityRecord', "Calls contract pushPhysicalActivityRecord function.")
    .addParam('d', 'Distance ran in meters')
    .addParam('s', 'Well sleept nights')
    .addParam('g', 'Gym visits')
    .setAction(async (taskArgs, hre) => {
        try {
            const contract = await getContract(hre, 'PhysicalActivityOracle');

            const registeredPublicKey = await contract.PUBLIC_KEY();
            const testPublicKey = await getRawPublicKey();

            // convert to ethers hex and then compare
            if (registeredPublicKey.x !== hre.ethers.hexlify(testPublicKey.x)) {
                const result1 = await contract.setPublicKey(testPublicKey, { attestationSha256: '0xfff', attestationChallenge: 'sAMple', attestationIpfsCID: 'bfci4' });
                result1.wait();
            }

            const runDistanceMeters = parseInt(taskArgs.d, 10);
            const healthySleepNights = parseInt(taskArgs.s, 10);
            const gymVisits = parseInt(taskArgs.g, 10);

            const running: RunningEventStruct = await signRunningEvent({
                timestamp: parseInt((+ new Date() / 1000).toFixed(0)),
                avgBpm: 125,
                distanceInMeters: parseInt(taskArgs.d, 10),
                paceInSecondsPerKm: 5 * 60
            });

            const gymVisit: GymVisitEventStruct = await signGymVisitEvent({
                timestamp: parseInt((+ new Date() / 1000).toFixed(0)),
                avgBpm: 130,
                maxBpm: 167,
                durationInMinutes: 55,
                location: { latitudeNanoDegree: -229034320n, longitudeNanoDegree: -432820980n }
            });

            const sleep: SleepEventStruct = await signSleepEvent({
                timestamp: parseInt((+ new Date() / 1000).toFixed(0)),
                avgBpm: 60,
                durationInMinutes: 4*60,
            });

            const result = await contract.publishPhysicalActivityEvent({
                gymVisit: [gymVisit],
                sleep: [sleep],
                running: [running]
            });

            const tx = await result.wait();
        } catch (err) {
            console.error(err);
        }
    })

task('EnforceVow', "Enforces the FitnessUnbreakableVow.")
    .setAction(async (taskArgs, hre) => {
        const contract = await getContract(hre, 'FitnessUnbreakableVow');

        const result = await contract.enforceAgreement();

        await result.wait();
    })

task('DeployPhysicalActivityOracle', "Deploys the PhysicalActivityOracle.")
    .addParam('startdate', 'Agreement start date', CREATION_DATE)
    .addParam('secondsinweek', 'Seconds in one week', SECONDS_IN_WEEK)
    .addParam('durationinweeks', 'Agreement duration in weeks', NUMBER_OF_CYLES)
    .setAction(async (taskArgs, hre) => {
        const { startDate, secondsInOneWeek, cronUpkeeperSpec } = getTimeSettings(taskArgs['startdate'], taskArgs['secondsinweek']);
        const numberOfCycles = parseInt(taskArgs['durationinweeks']);
        const expirationDate = startDate + secondsInOneWeek * numberOfCycles;

        const cronSpec96String = to96BytesString(hre, cronUpkeeperSpec);

        const { contractAddress: timeLordAddess } = await deployContract(
            hre,
            'TheDoctor',
            async (factory) => await factory.deploy(startDate, expirationDate, secondsInOneWeek, ...cronSpec96String)
        );

        const { contractAddress, contract } = await deployContract(
            hre,
            'PhysicalActivityOracle',
            async (factory) => await factory.deploy(timeLordAddess)
        );

        const testPublicKey = await getRawPublicKey();

        const result1 = await contract.setPublicKey(testPublicKey, { attestationSha256: '0xfff', attestationChallenge: 'sandbox', attestationIpfsCID: 'sandbox' });
        await result1.wait();

        const network = hre.network.name;

        if (network === 'localhost') {
            await installRip7212Mock(hre);

            return console.log('Skipping Etherscan verification for local network.');
        }

        try {
            await verify(network, 'PhysicalActivityOracle', contractAddress, [timeLordAddess]);
            await verify(network, 'TheDoctor', timeLordAddess, [startDate, expirationDate, secondsInOneWeek, ...cronSpec96String]);
        } catch (err) {
            console.error('Etherscan verification failed:', err);
        }
    })

task('DeployFitnessUnbreakableVow', "Deploys the FitnessUnbreakableVow")
    .addParam('stakedamount', 'Staked amount', STAKED_AMOUNT)
    .setAction(async (taskArgs, hre) => {
        const oracleAddress = getContractAddress('PhysicalActivityOracle', hre.network.name);
        const initialStake = hre.ethers.parseEther(taskArgs['stakedamount']);

        const { contract, contractAddress } = await deployContract(
            hre,
            'FitnessUnbreakableVow',
            async (factory) => await factory.deploy(oracleAddress, { value: initialStake })
        );

        const network = hre.network.name;

        if (network === 'localhost') return console.log('Skipping Etherscan verification for local network.');

        // Upkeeper is not enabled in sandbox
        // await FitnessUnbreakableVowUpkeeper.create(contract, hre);

        try {
            await verify(network, 'FitnessUnbreakableVow', contractAddress, [oracleAddress]);
        } catch (err) {
            console.error('Etherscan verification failed:', err);
        }
    })

task('BuildSchedulerParams', "Gather the necessary params to run a scheduler to save events emitted by the contract.")
    .setAction(async (taskArgs, hre) => {
        const theDoctor = await getContract(hre, 'TheDoctor');
        const oracle = await getContract(hre, 'PhysicalActivityOracle');

        const startDate = Number(await theDoctor.CREATION_DATE());
        const endDate = Number(await theDoctor.EXPIRATION_DATE() + await theDoctor.GRACE_PERIOD());
        const secondsInOneWeek = Number(await theDoctor.SECONDS_IN_ONE_WEEK()) + 5;
        const oracleAddress = await oracle.getAddress();

        fs.writeFileSync(
            path.join(__dirname, 'artifacts', 'scheduler-args.txt'),
            `RATE="rate(${Math.ceil(secondsInOneWeek / 60)} minutes)"\n` +
            `START_DATE="${new Date(1000 * (startDate + secondsInOneWeek)).toISOString()}"\n` +
            `END_DATE="${new Date(1000 * endDate).toISOString()}"\n` +
            `CONTRACT_ADDRESS="${oracleAddress}"\n`
        );
    })


task('ResetSandbox', "Used to reset the sandbox deployed in arbitrum sepolia.")
    .addParam('startdate', 'Agreement start date', CREATION_DATE)
    .addParam('secondsinweek', 'Seconds in one week', SECONDS_IN_WEEK)
    .addParam('durationinweeks', 'Agreement duration in weeks', NUMBER_OF_CYLES)
    .setAction(async (taskArgs, hre) => {
        const { startDate, secondsInOneWeek, cronUpkeeperSpec } = getTimeSettings(taskArgs['startdate'], taskArgs['secondsinweek']);
        const numberOfCycles = parseInt(taskArgs['durationinweeks']);
        const expirationDate = startDate + secondsInOneWeek * numberOfCycles;

        const contract = await getContract(hre, 'FitnessUnbreakableVow');

        const tx = await contract.reset(startDate, expirationDate);
        await tx.wait();
    })

const config: HardhatUserConfig = {
    solidity: {
        version: "0.8.28",
        settings: {
            viaIR: true,
        }
    },
    defaultNetwork: "hardhat",
    networks: {
        hardhat: {
            mining: {
                auto: false,
                interval: 1000, // Mine every 1s in real time just like a real network
            },
        },
        // Only include sepolia if credentials are available to avoid CI failures
        ...(SEPOLIA_RPC_URL && SEPOLIA_WALLET_PRIVATE_KEY ? {
            sepolia: {
                url: SEPOLIA_RPC_URL,
                accounts: [SEPOLIA_WALLET_PRIVATE_KEY]
            }
        } : {}),
        // Only include arbitrum if a private key is available
        ...(ARBITRUM_WALLET_PRIVATE_KEY ? {
            arbitrum: {
                url: "https://arb1.arbitrum.io/rpc",
                chainId: 42161,
                accounts: [ARBITRUM_WALLET_PRIVATE_KEY]
            }
        } : {}),
        ...(ARBITRUM_SEPOLIA_WALLET_PRIVATE_KEY ? {
            arbiSep: {
                url: ABI_SEP_RPC_URL,
                chainId: 421614,
                accounts: [ARBITRUM_SEPOLIA_WALLET_PRIVATE_KEY]
            }
        } : {})
    },
    etherscan: {
        apiKey: ETHERSCAN_API_KEY || '',
        customChains: [
            {
                network: "arbitrum",
                chainId: 42161,
                urls: {
                    apiURL: "https://api.etherscan.io/v2/api?chainid=42161",
                    browserURL: "https://arbiscan.io/",
                },
            },
            {
                network: "arbiSep",
                chainId: 421614,
                urls: {
                    apiURL: "https://api.etherscan.io/v2/api?chainid=421614",
                    browserURL: "https://sepolia.arbiscan.io/",
                },
            }
        ],
    },
    sourcify: {
        enabled: true,
    },
    gasReporter: {
        enabled: false,
        currency: 'USD',
        coinmarketcap: COINMARKETCAP_API_KEY,
    },
};

export default config;
