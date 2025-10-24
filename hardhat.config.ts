import '@nomicfoundation/hardhat-toolbox';
import * as dotenv from 'dotenv';
import { HardhatUserConfig, task } from "hardhat/config";
import { signPhysicalActivityRecord } from './scripts/utils';
import { getRawPublicKey } from './scripts/keys';
import { getContractAddress, saveContractAddress } from './scripts/addresses';
import setContractVersion from './scripts/set-contract-version';
import path from 'path';
import fs from 'fs';

// Defaults
const STAKED_AMOUNT = "0.001";
const CREATION_DATE = new Date().toISOString();
const NUMBER_OF_CYLES = "5.2";
const SECONDS_IN_WEEK = "120";

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
            const contractAddress = getContractAddress('PhysicalActivityOracle', hre.network.name);

            const contract = await hre.ethers.getContractAt("PhysicalActivityOracle", contractAddress);

            console.log(await contract.PUBLIC_KEY_ATTESTATION());
            console.log(await contract.PUBLIC_KEY());

            //const result = await contract.terminateVow();

            //await result.wait();
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
            const contractAddress = getContractAddress('PhysicalActivityOracle', hre.network.name);

            const contract = await hre.ethers.getContractAt("PhysicalActivityOracle", contractAddress);

            const registeredPublicKey = await contract.PUBLIC_KEY();
            const testPublicKey = await getRawPublicKey();

            // convert to ethers hex and then compare
            if (registeredPublicKey.x !== hre.ethers.hexlify(testPublicKey.x)) {
                const result1 = await contract.setPublicKey(testPublicKey, { attestationSha256: '', attestationChallenge: '', attestationIpfsCID: '' });
                result1.wait();
            }

            const runDistanceMeters = parseInt(taskArgs.d, 10);
            const healthySleepNights = parseInt(taskArgs.s, 10);
            const gymVisits = parseInt(taskArgs.g, 10);

            const record = { timestamp: Math.floor(+new Date() / 1000), runDistanceMeters, healthySleepNights, gymVisits };

            const { signature } = await signPhysicalActivityRecord(record);

            const result = await contract.pushPhysicalActivityRecord(signature, record);

            await result.wait();
        } catch (err) {
            console.error(err);
        }
    })

task('EnforceVow', "Enforces the FitnessUnbreakableVow.")
    .setAction(async (taskArgs, hre) => {
        try {
            const contractAddress = getContractAddress('FitnessUnbreakableVow', hre.network.name);

            const contract = await hre.ethers.getContractAt("FitnessUnbreakableVow", contractAddress);

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

        const PhysicalActivityOracle = await hre.ethers.getContractFactory("PhysicalActivityOracle");

        const contract = await PhysicalActivityOracle.deploy(creationDate, expirationDate, secondsInOneWeek);

        await contract.waitForDeployment();

        const { blockNumber = null } = await contract.deploymentTransaction()?.wait() || {};

        if (blockNumber == null) throw new Error('Unable to determine block number.');

        const contractAddress = await contract.getAddress();

        console.log(`PhysicalActivityOracle contract deployed to: ${contractAddress}`);

        saveContractAddress('PhysicalActivityOracle', contractAddress, hre.network.name, String(blockNumber));

        console.log('⚠️ Add the new contract address to ChainLink consumers list.');
        console.log('⚠️ Verify contract source code in Etherscan with: ');
        console.log(`npx hardhat verify --network ${hre.network.name} ${contractAddress} "${creationDate}" "${expirationDate}" "${secondsInOneWeek}"`);
    })

task('DeployFitnessUnbreakableVow', "Deploys the FitnessUnbreakableVow")
    .addParam('a', 'Staked amount', STAKED_AMOUNT)
    .setAction(async (taskArgs, hre) => {
        const oracleAddress = getContractAddress('PhysicalActivityOracle', hre.network.name);
        const chainLinkUpkeepAddress = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC"; //"0xb83E47C2bC239B3bf370bc41e1459A34b41238D0";

        const stakedAmount = taskArgs.a;

        console.log(stakedAmount);

        const oracle = await hre.ethers.getContractAt("PhysicalActivityOracle", oracleAddress);

        const creationDate = await oracle.CREATION_DATE();
        const expirationDate = await oracle.EXPIRATION_DATE();
        const secondsInOneWeek = await oracle.SECONDS_IN_ONE_WEEK();

        const FitnessUnbreakableVowFactory = await hre.ethers.getContractFactory('FitnessUnbreakableVow');
        const fitnessUnbreakableVow = await FitnessUnbreakableVowFactory.deploy(
            oracleAddress,
            chainLinkUpkeepAddress,
            creationDate,
            expirationDate,
            secondsInOneWeek,
            { value: hre.ethers.parseEther(stakedAmount) }
        );

        await fitnessUnbreakableVow.waitForDeployment();

        const { blockNumber = null } = await fitnessUnbreakableVow.deploymentTransaction()?.wait() || {};

        if (blockNumber == null) throw new Error('Unable to determine block number.');

        const contractAddress = await fitnessUnbreakableVow.getAddress();

        console.log(`FitnessUnbreakableVow contract deployed to: ${contractAddress}`);

        saveContractAddress('FitnessUnbreakableVow', contractAddress, hre.network.name, String(blockNumber));

        console.log('⚠️ Verify contract source code in Etherscan with: ');
        console.log(`npx hardhat verify --network ${hre.network.name} ${contractAddress} "${oracleAddress}" "${chainLinkUpkeepAddress}" "${creationDate}" "${expirationDate}" "${secondsInOneWeek}"`);
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
