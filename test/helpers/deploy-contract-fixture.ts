import hre from 'hardhat';
import { loadChainlinkMockContract } from './load-chainlink-mock-contract';
import ChainlinkServer from '../../scripts/chainlink-mock-server';
import { PUBLIC_KEY } from '../../scripts/keys';
import { FitnessUnbreakableVow } from '../../typechain-types';

export const STAKED_AMOUNT = hre.ethers.parseEther("8");
export const SEVEN_DAYS_IN_SECONDS = 60 * 60 * 24 * 7;
export const NUMBER_OF_CYLES = 4.2;
export const CONTRACT_VALIDITY_PERIOD = SEVEN_DAYS_IN_SECONDS * NUMBER_OF_CYLES; // 28 days;
export const CREATION_DATE = Math.floor(+new Date() / 1000);
export const EXPIRATION_DATE = CREATION_DATE + SEVEN_DAYS_IN_SECONDS * NUMBER_OF_CYLES;

export async function deployContractFixture() {
    const [owner, otherAccount, otherAccount2] = await hre.ethers.getSigners();

    const PhysicalActivityOracle = await hre.ethers.getContractFactory("PhysicalActivityOracle");
    const FitnessUnbreakableVow = await hre.ethers.getContractFactory("FitnessUnbreakableVow");

    const chainlinkMock = await loadChainlinkMockContract();

    const physicalActivityOracle = await PhysicalActivityOracle.deploy("localhost", CREATION_DATE, EXPIRATION_DATE, SEVEN_DAYS_IN_SECONDS);
    
    await physicalActivityOracle.waitForDeployment();

    console.log('[DeployContract] Set expiration date: ' + EXPIRATION_DATE);
    console.log('[DeployContract] Actual expiration date: ' + await physicalActivityOracle.EXPIRATION_DATE());

    const oracleAddress = await physicalActivityOracle.getAddress();
    const fitnessUnbreakableVow = await FitnessUnbreakableVow.deploy(
        oracleAddress,
        otherAccount2,
        await physicalActivityOracle.CREATION_DATE(),
        await physicalActivityOracle.EXPIRATION_DATE(),
        SEVEN_DAYS_IN_SECONDS,
        { value: STAKED_AMOUNT }
    );

    const transaction = await physicalActivityOracle.setPublicKey(PUBLIC_KEY);
    await transaction.wait();

    ChainlinkServer.setContract(chainlinkMock);

    return { fitnessUnbreakableVow, physicalActivityOracle, chainlinkMock, owner, otherAccount, otherAccount2 };
}


export async function calculatePenaltyAmount(contract: FitnessUnbreakableVow) {
    const creationDate = await contract.CREATION_DATE();
    const expirationDate = await contract.EXPIRATION_DATE();
    const stakedAmount = await contract.STAKED_AMOUNT();

    return stakedAmount / ((expirationDate - creationDate) / BigInt(SEVEN_DAYS_IN_SECONDS));
}