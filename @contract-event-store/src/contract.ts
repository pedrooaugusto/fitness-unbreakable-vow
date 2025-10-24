import { ethers } from 'ethers';
import { requireEnv } from './helpers';

const MIN_ABI = [
    { "inputs": [], "name": "getCurrentWeekIndex", "outputs": [{ "internalType": "uint8", "name": "", "type": "uint8" }], "stateMutability": "view", "type": "function" },
    { "inputs": [], "name": "SECONDS_IN_ONE_WEEK", "outputs": [{ "internalType": "uint256", "name": "", "type": "uint256" }], "stateMutability": "view", "type": "function" },
    { "inputs": [], "name": "CREATION_DATE", "outputs": [{ "internalType": "uint256", "name": "", "type": "uint256" }], "stateMutability": "view", "type": "function" }
];

export function loadContract(address?: string) {
    if (address == null) throw new Error('Contract address not provided');

    const rpcUrl = requireEnv('RPC_PROVIDER');
    const provider = new ethers.JsonRpcProvider(rpcUrl);

    return { contract: new ethers.Contract(address, MIN_ABI, provider), address: address };
}

export function parseEvent(topic0: string, topic1: string, data: string, transactionHash: string, blockNumber: number) {
    const weekIndex = Number(BigInt(topic1));

    const runDistanceMeters = Number(parseUintFromDataSlot(data, 0));
    const gymVisits = Number(parseUintFromDataSlot(data, 1));
    const healthySleepNights = Number(parseUintFromDataSlot(data, 2));

    return { weekIndex, runDistanceMeters, gymVisits, healthySleepNights, transactionHash, blockNumber };
}

function parseUintFromDataSlot(data: string, slotIndex: number): bigint {
    // data is 0x + 64*n hex chars; slotIndex is 0-based
    const clean = data.startsWith('0x') ? data.slice(2) : data;
    const start = slotIndex * 64;
    const end = start + 64;
    const slice = clean.slice(start, end);

    return BigInt('0x' + slice);
}
