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
import { deployContract, FitnessUnbreakableVowUpkeeper, getContract } from './scripts/utils';

// Defaults
const STAKED_AMOUNT = "0.0001";
const CREATION_DATE = new Date().toISOString();
const NUMBER_OF_CYLES = "3.2";
const SECONDS_IN_WEEK = (5 * 60).toString(); // 95min to run android test

dotenv.config();

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

        for (const file of ['console.sol', 'P256.sol']) {            
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
        try {
            const contract = await getContract(hre, 'FitnessUnbreakableVow');

            const result = await contract.terminateVow();

            await result.wait();
        } catch (err) {
            console.error(err);
        }
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
                avgBpm: 115,
                distanceInMeters: parseInt(taskArgs.d, 10),
                paceInSecondsPerKm: 5 * 60
            });

            const gymVisit: GymVisitEventStruct = await signGymVisitEvent({
                timestamp: parseInt((+ new Date() / 1000).toFixed(0)),
                avgBpm: 130,
                maxBpm: 167,
                durationInMinutes: 65,
                location: { latitudeNanoDegree: 0n, longitudeNanoDegree: 0n }
            });

            const sleep: SleepEventStruct = await signSleepEvent({
                timestamp: parseInt((+ new Date() / 1000).toFixed(0)),
                avgBpm: 60,
                durationInMinutes: 9*60,
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
        try {
            const contract = await getContract(hre, 'FitnessUnbreakableVow');

            const result = await contract.enforceAgreement();

            await result.wait();
        } catch (err) {
            console.error(err);
        }
    })

task('DeployPhysicalActivityOracle', "Deploys the PhysicalActivityOracle.")
    .addParam('s', 'Agreement start date', CREATION_DATE)
    .addParam('w', 'Agreement duration in weeks', NUMBER_OF_CYLES)
    .addParam('d', 'Seconds in one week', SECONDS_IN_WEEK)
    .setAction(async (taskArgs, hre) => {
        const creationDate = Math.floor(+new Date(taskArgs.s) / 1000);
        const numberOfCycles = parseFloat(taskArgs.w);
        const secondsInOneWeek = parseInt(taskArgs.d);
        const expirationDate = creationDate + secondsInOneWeek * numberOfCycles;

        const { contractAddress: timeLordAddess } = await deployContract(
            hre,
            'TheDoctor',
            async (factory) => await factory.deploy(creationDate, expirationDate, secondsInOneWeek)
        );

        const { contractAddress } = await deployContract(
            hre,
            'PhysicalActivityOracle',
            async (factory) => await factory.deploy(timeLordAddess)
        );

        console.log('⚠️ Add the new contract address to ChainLink consumers list.');
        console.log('⚠️ Verify contract source code in Etherscan with: ');
        console.log(`npx hardhat verify --network ${hre.network.name} ${contractAddress} "${timeLordAddess}"`);
        console.log(`npx hardhat verify --network ${hre.network.name} ${timeLordAddess} "${creationDate}" "${expirationDate}" "${secondsInOneWeek}"`);
    })

task('DeployFitnessUnbreakableVow', "Deploys the FitnessUnbreakableVow")
    .addParam('a', 'Staked amount', STAKED_AMOUNT)
    .setAction(async (taskArgs, hre) => {
        const oracleAddress = getContractAddress('PhysicalActivityOracle', hre.network.name);
        const initialStake = hre.ethers.parseEther(taskArgs.a);

        const { contract, contractAddress } = await deployContract(
            hre,
            'FitnessUnbreakableVow',
            async (factory) => await factory.deploy(oracleAddress, { value: initialStake })
        );

        if (hre.network.name !== 'localhost') {
            await FitnessUnbreakableVowUpkeeper.create(contract, await getContract(hre, 'TheDoctor'), hre);
        }

        console.log('⚠️ Verify contract source code in Etherscan with: ');
        console.log(`npx hardhat verify --network ${hre.network.name} ${contractAddress} "${oracleAddress}"`);
    })

task('config-upkeeper', "Config upkeeper")
    .setAction(async (taskArgs, hre) => {
        try {
            const contractAddress = getContractAddress('FitnessUnbreakableVow', hre.network.name);

            const contract = await hre.ethers.getContractAt("FitnessUnbreakableVow", contractAddress);
            const result2 = await contract.withdrawUpkeeperFunds();

            await result2.wait();
        } catch (err) {
            console.error(err);
            throw err;
        }
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
