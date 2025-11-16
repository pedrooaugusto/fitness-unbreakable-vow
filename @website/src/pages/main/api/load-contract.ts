import type { BytesLike, Networkish, Provider } from 'ethers';
import { Contract, ethers, Result } from 'ethers';
import type { Network } from '../types';

type EmittedEvent = ethers.EventLog & { args: unknown[] } | ethers.Log & { args: unknown[] };
type ParsedEventBase = { transactionHash: string; blockNumber: number; };

const RPC_URL_MAP: Record<Network, Networkish & { rpc: string[] }> = {
    localhost: {
        name: 'localhost',
        chainId: 31337,        
        rpc: [
            "http://192.168.0.105:8545",
            //"http://localhost:8545"
        ], // hardhat
    },
    sepolia: {
        name: 'sepolia',
        chainId: 11155111,
        rpc: [
            "https://ethereum-sepolia-rpc.publicnode.com",
            "https://1rpc.io/sepolia",
            "https://sepolia.gateway.tenderly.co"
        ]
    },
    arbitrum: {
        name: 'arbitrum',
        chainId: 42161,
        rpc: [
            "https://arb1.arbitrum.io/rpc",
            "https://rpc.ankr.com/arbitrum",
            "https://arbitrum.publicnode.com"
        ]
    },
    arbiSep: {
        name: 'arbitrum-sepolia',
        chainId: 421614,
        rpc: [
            "https://arbitrum-sepolia-rpc.publicnode.com",
        ]
    },
};

export type EnhancedContract = {
    network: Network;
    contractAddress: string;
    getEvents: <T extends ParsedEventBase>(eventName: string, indexes: string[], data: string[], fromBlock?: number, toBlock?: number) => Promise<T[]>
} & Contract;

// Same address on Ethereum mainnet / Arbitrum / Sepolia
const MULTICALL_ADDRESS = "0xcA11bde05977b3631167028862bE2a173976CA11";

let addresses: Record<string, string> | null = null;
let provider: Provider | null = null;
let network: Network | null = null;
let PhysicalActivityOracle: EnhancedContract | null = null;
let FitnessUnbreakableVow: EnhancedContract | null = null;
let TimeLordContract: EnhancedContract | null = null;
let MulticallContract: Contract | null = null;
let fromBlock: number | null = null;

export default async function loadContract() {
    addresses ||= await fetchAdresses();
    network = addresses.LastUsedNetwork as Network;    
    fromBlock = Number(addresses[`${network}.blockNumber`]);
    provider ||= makeProviderWithFallback(network);
    PhysicalActivityOracle ||= await getContract('PhysicalActivityOracle', network, provider) as EnhancedContract;
    FitnessUnbreakableVow ||= await getContract('FitnessUnbreakableVow', network, provider) as EnhancedContract;
    TimeLordContract ||= await getContract('TheDoctor', network, provider, await FitnessUnbreakableVow.TIME_LORD()) as EnhancedContract;    
    MulticallContract ||= await getContract('Multicall3', network, provider, MULTICALL_ADDRESS);

    FitnessUnbreakableVow.network = network;
    FitnessUnbreakableVow.contractAddress = addresses[`${network}.FitnessUnbreakableVow`];
    FitnessUnbreakableVow.getEvents = (<T extends ParsedEventBase>(eventName: string, indexes: string[], data: string[], fromBlock1 = fromBlock!, toBlock1?: number) => {
        return getEvents<T>(FitnessUnbreakableVow as Contract, eventName, indexes, data, fromBlock1, toBlock1);
    }) as any;

    PhysicalActivityOracle.network = network;
    PhysicalActivityOracle.contractAddress = addresses[`${network}.PhysicalActivityOracle`];
    PhysicalActivityOracle.getEvents = (<T extends ParsedEventBase>(eventName: string, indexes: string[], data: string[], fromBlock1 = fromBlock!, toBlock1?: number) => {
        return getEvents<T>(PhysicalActivityOracle as Contract, eventName, indexes, data, fromBlock1, toBlock1);
    }) as any;

    return {
        PhysicalActivityOracle,
        FitnessUnbreakableVow,
        TimeLordContract,
        getBalance: (target: Contract) => getBalance(target, provider!),
        executeMulticall: (contract: Contract, functions: string[]) => executeMulticall(contract, functions, network!)
    }
}

async function fetchAdresses() {
    const response = await fetch('/addresses?t=' + new Date().getTime());

    const text = await response.text();
    const keyPair = text.split('\n').map(line => line.split('=').map(str => str.trim()));

    return keyPair.reduce((record: Record<string, string>, [key, value]) => {
        record[key] = value;

        return record;
    }, {});
}

async function getContract(name: string, network: string, provider: Provider, targetAddress?: string) {
    const address = targetAddress || addresses![`${network}.${name}`];
    const { abi } = await (await fetch(`/abi/${name}.json`)).json();

    return new ethers.Contract(address, abi, provider);
}

async function executeMulticall(contract: Contract, functions: string[], network: Network) {
    if (network === 'localhost') return await localhostMultiCall(contract, functions);

    const contractInterface = contract.interface;
    const calls = functions.map(functionName => ({ target: contract, callData: contractInterface.encodeFunctionData(functionName) }));

    const [, returnData]: unknown[][] = await MulticallContract!.aggregate!(calls);

    return functions.map((functionName, index) => {
        const decodedResult = contractInterface.decodeFunctionResult(functionName, returnData[index] as BytesLike);

        if (decodedResult.length === 1) return decodedResult[0];

        return decodedResult;
    });
}

async function localhostMultiCall(contract: Contract, functions: string[]) {
    const results: Array<unknown> = [];

    for (let i = 0; i < functions.length; i++) {
        results.push(await (contract[functions[i] as string] as Function)() as Result)
    }

    return results;
}

async function getBalance(target: Contract, provider: Provider) {
    return await provider.getBalance(await target.getAddress());
}

function makeProviderWithFallback(networkName: Network): Provider {
    const network = RPC_URL_MAP[networkName];

    const providers = network.rpc.map((url, i) => ({
        provider: new ethers.JsonRpcProvider(url, network, { staticNetwork: true }),
        priority: i + 1,
        stallTimeout: 10000, // ms before trying next
        weight: 1,
    }));

    return new ethers.FallbackProvider(providers, network, { quorum: 1 });
}

async function getEvents<T>(contract: Contract, eventName: string, indexes: string[], data: string[], fromBlock: number, toBlock?: number) {
    const filter = contract.filters[eventName](...indexes)!;

    const events = (await contract.queryFilter(filter, fromBlock, toBlock) as EmittedEvent[]) || [];

    return events.map((emittedEvent) => {
        const eventParsedArgs: T = {} as T;

        for (let i = 0; i < emittedEvent.args.length; i++) {
            const keyName = data[i]!;
            const value = emittedEvent.args[i];

            (eventParsedArgs as Record<string, unknown>)[keyName] = typeof value === 'bigint' ? Number(value) : value;
        }

        return {
            transactionHash: emittedEvent.transactionHash,
            blockNumber: emittedEvent.blockNumber,
            ...eventParsedArgs,
        };
    });
}