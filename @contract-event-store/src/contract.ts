import { ethers, Interface } from 'ethers';
import { requireEnv } from './helpers';
import MIN_ABI from './abi.json';
import { LogEntry } from './etherscan';

export interface PhysicalActivityStatsUpdate {
    transactionHash: string;
    blockNumber: number;
    weekIndex: number;
    stats: {
        timestamp: number;
        running: any;
        gym: any;
        sleep: any;
    }
}

const PhysicalActivityOracleInterface = new Interface(MIN_ABI);

export function loadContract(address?: string) {
    if (address == null) throw new Error('Contract address not provided');

    const rpcUrl = requireEnv('RPC_PROVIDER');
    const provider = new ethers.JsonRpcProvider(rpcUrl);

    return { contract: new ethers.Contract(address, MIN_ABI, provider), address: address };
}

export function parsePhysicalActivityStatsUpdateEvent(etherScanLogs: LogEntry[]) {
    const decoded: PhysicalActivityStatsUpdate[] = [];

    for (const logEntry of etherScanLogs) {
        try {
            const parsed = PhysicalActivityOracleInterface.parseLog({ topics: logEntry.topics, data: logEntry.data });

            const stats: PhysicalActivityStatsUpdate['stats'] = {
                timestamp: parsed!.args.stats.toObject().timestamp,
                sleep: parsed!.args.stats.sleep.toObject(),
                gym: parsed!.args.stats.gym.toObject(),
                running: parsed!.args.stats.running.toObject()
            }

            decoded.push({
                weekIndex: parsed!.args.weekIndex,
                stats: stats,
                transactionHash: logEntry.transactionHash,
                blockNumber: Number(logEntry.blockNumber)
            });

        } catch (err) {
            // nothing tot do...
            console.error('Unable to parse event: ', err);

            throw err;
        }
    }

    return decoded;
}
