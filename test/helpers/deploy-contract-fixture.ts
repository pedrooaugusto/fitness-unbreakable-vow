import hre from 'hardhat';
import { getRawPublicKey } from '../../scripts/keys';
import { FitnessUnbreakableVow } from '../../typechain-types';

export const STAKED_AMOUNT = hre.ethers.parseEther("36");
export const SEVEN_DAYS_IN_SECONDS = 60 * 60 * 24 * 7;
export const NUMBER_OF_CYLES = 12.2;
export const CONTRACT_VALIDITY_PERIOD = SEVEN_DAYS_IN_SECONDS * NUMBER_OF_CYLES; // 3 months;
export const CREATION_DATE = Math.floor(+new Date() / 1000);
export const EXPIRATION_DATE = CREATION_DATE + SEVEN_DAYS_IN_SECONDS * NUMBER_OF_CYLES;

export async function deployContractFixture() {
    const [owner, otherAccount, otherAccount2] = await hre.ethers.getSigners();

    const PhysicalActivityOracle = await hre.ethers.getContractFactory("PhysicalActivityOracle");
    const FitnessUnbreakableVow = await hre.ethers.getContractFactory("FitnessUnbreakableVow");

    const physicalActivityOracle = await PhysicalActivityOracle.deploy(CREATION_DATE, EXPIRATION_DATE, SEVEN_DAYS_IN_SECONDS);
    
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

    await fitnessUnbreakableVow.waitForDeployment();

    const transaction = await physicalActivityOracle.setPublicKey(await getRawPublicKey(), { attestationChallenge: 'test',attestationIpfsCID: '123', attestationSha256: '0x909' });
    await transaction.wait();

    return { fitnessUnbreakableVow, physicalActivityOracle, owner, otherAccount, otherAccount2 };
}


export async function calculatePenaltyAmount(contract: FitnessUnbreakableVow) {
    const creationDate = await contract.CREATION_DATE();
    const expirationDate = await contract.EXPIRATION_DATE();
    const stakedAmount = await contract.STAKED_AMOUNT();

    return stakedAmount / ((expirationDate - creationDate) / BigInt(SEVEN_DAYS_IN_SECONDS));
}