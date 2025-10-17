import '@nomicfoundation/hardhat-toolbox';
import * as dotenv from 'dotenv';
import { HardhatUserConfig, task } from "hardhat/config";
import { connectOrDeploy, signPhysicalActivityRecord } from './scripts/utils';
import { executeCode } from './scripts/execute-code-oracle';
import { PUBLIC_KEY } from './scripts/keys';
import minifySignatureVerifierSourceCode from './scripts/minify-verifier-script';
import { getContractAddress, saveContractAddress } from './scripts/addresses';
import { ChainlinkFunctionsMock } from './typechain-types/contracts/lib/mock/standard/ChainlinkFunctionsMock.sol';
import setContractVersion from './scripts/set-contract-version';
import path from 'path';
import fs from 'fs';

const CREATION_DATE = Math.floor(+new Date() / 1000);
const NUMBER_OF_CYLES = 5.2;
const SECONDS_IN_WEEK = 3600;
const EXPIRATION_DATE = CREATION_DATE + SECONDS_IN_WEEK * NUMBER_OF_CYLES;

dotenv.config();

// Optional environment variables (may be missing in CI)
const SEPOLIA_RPC_URL = process.env['sepolia.RPC_URL'];
const SEPOLIA_WALLET_PRIVATE_KEY = process.env['sepolia.WALLET_PRIVATE_KEY'];
const ARBITRUM_WALLET_PRIVATE_KEY = process.env['arbitrum.WALLET_PRIVATE_KEY'];
const ETHERSCAN_API_KEY_SEPOLIA = process.env['sepolia.ETHERSCAN.API_KEY'];
const ETHERSCAN_API_KEY_ARBITRUM = process.env['arbitrum.ETHERSCAN.API_KEY'];
const COINMARKETCAP_API_KEY = process.env['GAS_REPORTER.COIN_MARKET_API_KEY'];

task('pre:compile')
    .setAction(async(_, { network: { name: networkName } }) => {
        const isLocalhost = networkName === 'localhost' || networkName === 'hardhat';
        const source = path.join(__dirname, 'contracts', 'lib', 'mock', isLocalhost ? 'standard' : 'empty');
        const destination = path.join(source, '..');

        for (const file of ['console.sol', 'ChainlinkFunctionsMock.sol']) {            
            fs.copyFileSync(path.join(source, file + '.source'), path.join(destination, file));
        }

        console.log(`🔁 Using ${isLocalhost ? 'standard' : 'empty'} mocks for network ${networkName}`);
})

task("compile")
    .setAction(async (args, hre, runSuper) => {
        minifySignatureVerifierSourceCode()
        setContractVersion()
        hre.run('pre:compile')

        await runSuper(args);
    });

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

task('DeployPhysicalActivityOracle', "Deploys the PhysicalActivityOracle.")
    .setAction(async (taskArgs, hre) => {
        const networkName = hre.network.name.toUpperCase();

        const PhysicalActivityOracle = await hre.ethers.getContractFactory("PhysicalActivityOracle");

        const contract = await PhysicalActivityOracle.deploy(networkName, CREATION_DATE, EXPIRATION_DATE, SECONDS_IN_WEEK);

        await contract.waitForDeployment();

        const { blockNumber = null } = await contract.deploymentTransaction()?.wait() || {};

        if (blockNumber == null) throw new Error('Unable to determine block number.');

        const contractAddress = await contract.getAddress();

        console.log(`PhysicalActivityOracle contract deployed to: ${contractAddress}`);

        saveContractAddress('PhysicalActivityOracle', contractAddress, hre.network.name, String(blockNumber));

        console.log('⚠️ Add the new contract address to ChainLink consumers list.');
        console.log('⚠️ Verify contract source code in Etherscan with: ');
        console.log(`npx hardhat verify --network ${hre.network.name} ${contractAddress} "${networkName}" "${CREATION_DATE}" "${EXPIRATION_DATE}" "${SECONDS_IN_WEEK}"`);
    })

task('DeployFitnessUnbreakableVow', "Deploys the FitnessUnbreakableVow")
    .setAction(async (taskArgs, hre) => {
        const STAKED_AMOUNT = hre.ethers.parseEther("0.01");// 0.001
        const oracleAddress = getContractAddress('PhysicalActivityOracle', hre.network.name);
        const chainLinkUpkeepAddress = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC"; //"0xb83E47C2bC239B3bf370bc41e1459A34b41238D0";

        const FitnessUnbreakableVowFactory = await hre.ethers.getContractFactory('FitnessUnbreakableVow');
        const oracle = await hre.ethers.getContractAt("PhysicalActivityOracle", oracleAddress);
        const fitnessUnbreakableVow = await FitnessUnbreakableVowFactory.deploy(
            oracleAddress,
            chainLinkUpkeepAddress,
            await oracle.CREATION_DATE(),
            await oracle.EXPIRATION_DATE(),
            SECONDS_IN_WEEK,
            { value: STAKED_AMOUNT }
        );

        await fitnessUnbreakableVow.waitForDeployment();

        const { blockNumber = null } = await fitnessUnbreakableVow.deploymentTransaction()?.wait() || {};

        if (blockNumber == null) throw new Error('Unable to determine block number.');

        const contractAddress = await fitnessUnbreakableVow.getAddress();

        console.log(`FitnessUnbreakableVow contract deployed to: ${contractAddress}`);

        saveContractAddress('FitnessUnbreakableVow', contractAddress, hre.network.name, String(blockNumber));

        console.log('⚠️ Verify contract source code in Etherscan with: ');
        console.log(`npx hardhat verify --network ${hre.network.name} ${contractAddress} "${oracleAddress}" "${chainLinkUpkeepAddress}" "${CREATION_DATE}" "${EXPIRATION_DATE}" "${SECONDS_IN_WEEK}"`);
    })

task('MockChainLinkOracle', "Deploys an mock Chainlink oracle.")
    .setAction(async (taskArgs, hre) => {
        const contract = await connectOrDeploy(getContractAddress('ChainlinkFunctionsMock', hre.network.name), 'ChainlinkFunctionsMock', hre) as ChainlinkFunctionsMock;

        const contractAddress = await contract.getAddress();
        saveContractAddress('ChainlinkFunctionsMock', contractAddress, hre.network.name, '');

        console.log(`ChainlinkFunctionsMock contract deployed to: ${contractAddress}`);

        console.log('\n\n⚠️ Starting server to listen for requests to execute code.');

        contract.on(contract.filters.ExcuteCodeRequest(), async(arg: any) => {
            console.log('Received request to execute code: ', arg.args);
            const code = await contract.getCodeToExecute();
            const args = await contract.getCodeToExecuteArgs();

            const output = await executeCode(code, args);

            const response = await contract.setCodeToExecuteResponse(output);

            await response.wait();
        });

        contract.on(contract.filters.ExcuteCodeResponse(), async(arg: any) => {
            console.log('Received execute code response: ', arg.args);
            console.log('==================================================================================');
        });

        await new Promise(() => {});
    })

const config: HardhatUserConfig = {
    solidity: {
        version: "0.8.28",
    },
    defaultNetwork: "hardhat",
    networks: {
        hardhat: {
            mining: {
                auto: false,
                interval: 1000, // Mine every 1s in real time just like a real network
            }
        },
        ganache: {
            url: "http://127.0.0.1:7545",
            chainId: 1337
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
    },
    etherscan: {
        apiKey: ETHERSCAN_API_KEY_SEPOLIA || '',
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
        coinmarketcap: COINMARKETCAP_API_KEY,
    },
};

export default config;
