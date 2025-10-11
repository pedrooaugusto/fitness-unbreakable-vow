import hre from 'hardhat';

export const HARDHAT_FIRST_ADDRESS = "0x5FbDB2315678afecb367f032d93F642f64180aa3";

export async function loadChainlinkMockContract() {
    const code = await hre.ethers.provider.getCode(HARDHAT_FIRST_ADDRESS);
    
    if (code === '0x') {        
        const ChainlinkFunctionsMock = await hre.ethers.getContractFactory("ChainlinkFunctionsMock");

        const chainlinkMock = await ChainlinkFunctionsMock.deploy();

        await chainlinkMock.waitForDeployment();

        return chainlinkMock;
    }

    return await hre.ethers.getContractAt("ChainlinkFunctionsMock", HARDHAT_FIRST_ADDRESS);
}