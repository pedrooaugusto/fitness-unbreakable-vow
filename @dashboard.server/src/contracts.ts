import { Contract, ethers, Result } from 'ethers';
import { abi as PhysicalActivityOracleAbi } from './abi/PhysicalActivityOracle.json';
import { abi as FitnessUnbreakableVowAbi } from './abi/FitnessUnbreakableVow.json';
import { abi as MulticallAbi } from './abi/Multicall3.json';
import { __LastUsedNetwork, FitnessUnbreakableVow as FitnessUnbreakableVowAddresses, PhysicalActivityOracle as PhysicalActivityOracleAddresses } from './abi/addresses.json';
import * as dotenv from 'dotenv';

dotenv.config();

const NETWORK = __LastUsedNetwork as keyof typeof FitnessUnbreakableVowAddresses;
const RPC_URL = process.env[NETWORK + '.RPC_URL']

export const FitnessUnbreakableVowAddress = FitnessUnbreakableVowAddresses[NETWORK];
export const PhysicalActivityOracleAddress = PhysicalActivityOracleAddresses[NETWORK];
export const MULTICALL_ADDRESS = "0xcA11bde05977b3631167028862bE2a173976CA11"; // Ethereum mainnet / Arbitrum / Sepolia deploy

export const provider = new ethers.JsonRpcProvider(RPC_URL);
export const PhysicalActivityOracle = new ethers.Contract(PhysicalActivityOracleAddress, PhysicalActivityOracleAbi, provider);
export const FitnessUnbreakableVow = new ethers.Contract(FitnessUnbreakableVowAddress, FitnessUnbreakableVowAbi, provider);
export const MulticallContract = new ethers.Contract(MULTICALL_ADDRESS, MulticallAbi, provider);

export async function executeMulticall(contract: Contract, functions: string[]) {
    if (NETWORK === 'localhost') return await localhostMultiCall(contract, functions);

    const contractInterface = contract.interface;
    const calls = functions.map(functionName => ({ target: contract, callData: contractInterface.encodeFunctionData(functionName) }));

    const [, returnData]: any = await MulticallContract.aggregate!(calls);

    return functions.map((functionName, index) => {
        const decodedResult = contractInterface.decodeFunctionResult(functionName, returnData[index]);

        if (decodedResult.length === 1) return decodedResult[0];

        return decodedResult;
    });
}

async function localhostMultiCall(contract: Contract, functions: string[]) {
    const results: Array<any> = [];
    
    for (let i = 0; i < functions.length; i++) {
        results.push(await (contract[functions[i] as string] as Function)() as Result)
    }

    return results;
}

export async function getBalance(target: Contract) {
    return await provider.getBalance(await target.getAddress());
}