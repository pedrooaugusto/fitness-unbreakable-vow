import { requireEnv } from './helpers';

const NetworkChainIdMap: Record<string, number> = { 
    'arbitrum': 42161,
    'sepolia': 11155111,
    'localhost': 31337,
    'arbiSep': 421614,
};

export type LogEntry = {
    address: string;
    topics: string[];
    data: string;
    blockNumber: string;
    timeStamp: string;
    gasPrice: string;
    gasUsed: string;
    logIndex: string;
    transactionHash: string;
    transactionIndex: string;
};

export type EtherscanResponse<T> = {
    status: string;
    message: string;
    result: T;
};

export async function getEvents(contractAddress: string, eventTopic: string, network: string, weekIndex: number, fromBlock: number, toBlock: number): Promise<LogEntry[]> {
    const base = requireEnv('ETHERSCAN_URL');
    const apiKey = requireEnv('ETHERSCAN_API_KEY');
    const chainId = NetworkChainIdMap[network].toString();

    const url = new URL(base);
    url.searchParams.set('module', 'logs');
    url.searchParams.set('action', 'getLogs');
    url.searchParams.set('fromBlock', String(fromBlock));
    url.searchParams.set('toBlock', String(toBlock));
    url.searchParams.set('address', contractAddress);
    url.searchParams.set('chainid', chainId);
    url.searchParams.set('topic0', eventTopic);
    url.searchParams.set('topic0_1_opr', 'and');
    url.searchParams.set('topic1', '0x' + weekIndex.toString(16).padStart(64, '0'));
    url.searchParams.set('page', '0');
    url.searchParams.set('offset', '1000');
    url.searchParams.set('apikey', apiKey);

    const response = await fetch(url.toString());

    if (!response.ok) throw new Error(`Etherscan getLogs failed: HTTP ${response.status}`);

    const payload = await response.json() as EtherscanResponse<LogEntry[]>;

    if (payload.status !== '1') return [];

    return payload.result;
}

export async function getBlockNumber(timestampSeconds: number, network: string): Promise<number> {
    const base = requireEnv('ETHERSCAN_URL');
    const apiKey = requireEnv('ETHERSCAN_API_KEY');
    const chainId = NetworkChainIdMap[network].toString();

    const url = new URL(base);
    url.searchParams.set('module', 'block');
    url.searchParams.set('action', 'getblocknobytime');
    url.searchParams.set('chainid', chainId);
    url.searchParams.set('timestamp', String(timestampSeconds));
    url.searchParams.set('closest', 'before');
    url.searchParams.set('apikey', apiKey);

    const response = await fetch(url.toString());

    if (!response.ok) throw new Error(`Etherscan blockByTime failed: HTTP ${response.status}`);
    
    const payload = await response.json() as EtherscanResponse<string>;

    if (payload.status !== '1') throw new Error(`Etherscan blockByTime error: ${payload.message}`);

    return Number(payload.result);
}