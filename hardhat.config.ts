import '@nomicfoundation/hardhat-toolbox';
import { HardhatUserConfig, task } from "hardhat/config";
import path from 'path';
import fs from 'fs';
import { signPhysicalActivityRecord } from './scripts/utils';
import { executeCode } from './scripts/execute-code-oracle';
import { PUBLIC_KEY } from './scripts/keys';
import * as dotenv from 'dotenv';

dotenv.config();

function getContractAddress(contractName: string, network: string) {
    const addressesFile = path.join(__dirname, "contracts", ".addresses");

    return JSON.parse(fs.readFileSync(addressesFile, "utf8"))[contractName][network];
}

function saveContractAddress(contractName: string, contractAddress: string, network: string) {
    const addressesFile = path.join(__dirname, "contracts", ".addresses");
    const deployedAddresses = JSON.parse(fs.readFileSync(addressesFile, "utf8"));

    deployedAddresses[contractName] = { ...deployedAddresses[contractName] || {}, [network]: contractAddress };
    deployedAddresses['__LastUsedNetwork'] = network;

    fs.writeFileSync(addressesFile, JSON.stringify(deployedAddresses, null, 2));
}

task('GetPhysicalActivityRecord', "Queries dailySteps from oracle `PhysicalActivityOracle`.")
    .setAction(async (taskArgs, hre) => {
        const contractAddress = getContractAddress('PhysicalActivityOracle', hre.network.name);

        const contract = await hre.ethers.getContractAt("PhysicalActivityOracle", contractAddress);

        const record = await contract.listAllPhysicalActivityRecords();

        console.log('Physical Activity Record: ', record);
    })

task('TerminateVow', "Terminates the FitnessUnbreakableVow.")
    .setAction(async (taskArgs, hre) => {
        try {
            const contractAddress = getContractAddress('FitnessUnbreakableVow', hre.network.name);

            const contract = await hre.ethers.getContractAt("FitnessUnbreakableVow", contractAddress);

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
            const contractAddress = getContractAddress('PhysicalActivityOracle', hre.network.name);

            const contract = await hre.ethers.getContractAt("PhysicalActivityOracle", contractAddress);

            if (await contract.BASE64_PUBLIC_KEY() !== PUBLIC_KEY) {
                const result1 = await contract.setPublicKey(PUBLIC_KEY);
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


const CREATION_DATE = Math.floor(+new Date() / 1000);
const NUMBER_OF_CYLES = 6.2;
const SECONDS_IN_WEEK = 60 * 10;
const EXPIRATION_DATE = CREATION_DATE + SECONDS_IN_WEEK * NUMBER_OF_CYLES;

task('DeployPhysicalActivityOracle', "Deploys the PhysicalActivityOracle.")
    .setAction(async (taskArgs, hre) => {
        const networkName = hre.network.name.toUpperCase();

        const PhysicalActivityOracle = await hre.ethers.getContractFactory("PhysicalActivityOracle");

        const contract = await PhysicalActivityOracle.deploy(networkName, CREATION_DATE, EXPIRATION_DATE);

        await contract.waitForDeployment();

        const contractAddress = await contract.getAddress();

        console.log(`PhysicalActivityOracle contract deployed to: ${contractAddress}`);

        saveContractAddress('PhysicalActivityOracle', contractAddress, hre.network.name);

        console.log('⚠️ Add the new contract address to ChainLink consumers list.');
        console.log('⚠️ Verify contract source code in Etherscan with: ');
        console.log(`npm run verify:${hre.network.name} ${contractAddress} "${networkName}" "${CREATION_DATE}" "${EXPIRATION_DATE}"`);
    })

task('DeployFitnessUnbreakableVow', "Deploys the FitnessUnbreakableVow")
    .setAction(async (taskArgs, hre) => {
        const STAKED_AMOUNT = hre.ethers.parseEther("0.001");
        const oracleAddress = getContractAddress('PhysicalActivityOracle', hre.network.name);
        const chainLinkUpkeepAddress = "0xb83E47C2bC239B3bf370bc41e1459A34b41238D0";

        const FitnessUnbreakableVowFactory = await hre.ethers.getContractFactory('FitnessUnbreakableVow');
        const oracle = await hre.ethers.getContractAt("PhysicalActivityOracle", oracleAddress);
        const fitnessUnbreakableVow = await FitnessUnbreakableVowFactory.deploy(
            oracleAddress,
            chainLinkUpkeepAddress,
            await oracle.CREATION_DATE(),
            await oracle.EXPIRATION_DATE(),
            { value: STAKED_AMOUNT }
        );

        await fitnessUnbreakableVow.waitForDeployment();

        const contractAddress = await fitnessUnbreakableVow.getAddress();

        console.log(`FitnessUnbreakableVow contract deployed to: ${contractAddress}`);

        saveContractAddress('FitnessUnbreakableVow', contractAddress, hre.network.name);

        console.log('⚠️ Verify contract source code in Etherscan with: ');
        console.log(`npm run verify:${hre.network.name} ${contractAddress} "${oracleAddress}" "${CREATION_DATE}" "${EXPIRATION_DATE}"`);
    })

task('SetUpkeepAddress', "Add the upkeep")
    .addParam('a', 'Upkeep address')
    .setAction(async (taskArgs, hre) => {
        const contractAddress = getContractAddress('FitnessUnbreakableVow', hre.network.name);

        const contract = await hre.ethers.getContractAt("FitnessUnbreakableVow", contractAddress);

        //const result = await contract.setChainlinkUpkeepAddress(taskArgs.a);

        //await result.wait();
    })

task('MockChainLinkOracle', "Deploys an mock Chainlink oracle.")
    .setAction(async (taskArgs, hre) => {
        const ChainlinkFunctionsMock = await hre.ethers.getContractFactory("ChainlinkFunctionsMock");
        const contract = await ChainlinkFunctionsMock.deploy();

        await contract.waitForDeployment();

        const contractAddress = await contract.getAddress();
        saveContractAddress('ChainlinkFunctionsMock', contractAddress, hre.network.name);

        console.log(`ChainlinkFunctionsMock contract deployed to: ${contractAddress}`);

        console.log('\n\n⚠️ Starting server to listen for requests to execute code.');

        while (true) {
            const hasCodeToExecute = await contract.hasCodeToExecute();

            console.log('Has Query to execute: ', hasCodeToExecute);

            if (hasCodeToExecute) {
                console.log('\tExecuting code.');

                const code = await contract.getCodeToExecute();
                const args = await contract.getCodeToExecuteArgs();

                const output = await executeCode(code, args);

                const response = await contract.setCodeToExecuteResponse(output);

                await response.wait();
            }

            await wait(3000);
        }
    })

const config: HardhatUserConfig = {
    solidity: "0.8.28",
    networks: {
        hardhat: {
            mining: {
                auto: true, // Disable automining
                interval: 5000, // Mine a new block every 5 seconds (in ms)
            }
        },
        sepolia: {
            url: process.env['sepolia.RPC_URL'],
            accounts: [process.env['test.WALLET_PRIVATE_KEY']!]
        },
        ganache: {
            url: "http://127.0.0.1:7545",
            chainId: 1337
        },
        arbitrum: {
            url: "https://arb1.arbitrum.io/rpc",
            chainId: 42161,
            accounts: [process.env['prod.WALLET_PRIVATE_KEY']!]
        }
    },
    etherscan: {
        apiKey: {
            sepolia: process.env['sepolia.ETHERSCAN.API_KEY']!,
            arbitrum: process.env['arbitrum.ETHERSCAN.API_KEY']!
        },
        customChains: [
            {
                network: "arbitrum",
                chainId: 42161,
                urls: {
                    apiURL: "https://api.arbiscan.io/api",
                    browserURL: "https://arbiscan.io/",
                },
            },
        ],
    },
    sourcify: {
        enabled: true,
    },
    gasReporter: {
        enabled: false,
        currency: 'USD',
        coinmarketcap: process.env['GAS_REPORTER.COIN_MARKET_API_KEY']!,
    }
};

const wait = (t: number) => new Promise((res, rej) => setTimeout(res, t));

export default config;
