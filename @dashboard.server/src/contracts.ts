import * as dotenv from 'dotenv';
import path from 'path';
import { Contract, ethers, Result } from 'ethers';
import { abi as PhysicalActivityOracleAbi } from './abi/PhysicalActivityOracle.json';
import { abi as FitnessUnbreakableVowAbi } from './abi/FitnessUnbreakableVow.json';
import { abi as MulticallAbi } from './abi/Multicall3.json';
import { Network } from './types';

dotenv.config();
dotenv.config({ path:  path.join(__dirname, 'abi', '.addresses')});

export const NETWORK = process.env.LastUsedNetwork as Network;
const RPC_URL = process.env[NETWORK + '.RPC_URL'];

export const FitnessUnbreakableVowAddress = process.env[NETWORK + '.' + 'FitnessUnbreakableVow']!;
export const PhysicalActivityOracleAddress = process.env[NETWORK + '.' + 'PhysicalActivityOracle']!;
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

export async function getEvents<T>(contract: Contract, eventName: string, indexes: any[], data: string[]) {
    const filter = contract.filters[eventName]?.(...indexes)!;

    const events = await contract.queryFilter(filter);

    return events.map(item => {
        const args: T = {} as any;
        for (let i = 0; i < (item as any).args.length; i++) {
            const keyName = data[i]!;
            const value = (item as any).args[i];

            (args as any)[keyName] = typeof value === 'bigint' ? Number(value) : value;
        }

        return {
            transactionHash: item.transactionHash,
            blockNumber: item.blockNumber,
            ...args,
        };
    });
}