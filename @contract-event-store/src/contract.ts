import { ethers, Interface } from 'ethers';
import { requireEnv } from './helpers';
import { abi as PhysicalActivityOracleAbi } from './abi/PhysicalActivityOracle.json';
import { abi as TimeLordAbi } from './abi/TheDoctor.json';
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

const PhysicalActivityOracleInterface = new Interface(PhysicalActivityOracleAbi);

export async function loadContracts(physicalActivityOracleAddress?: string) {
    if (physicalActivityOracleAddress == null) throw new Error('Contract address not provided');

    const rpcUrl = requireEnv('RPC_PROVIDER');
    const provider = new ethers.JsonRpcProvider(rpcUrl);
    const physicalActivityOracle = new ethers.Contract(physicalActivityOracleAddress, PhysicalActivityOracleAbi, provider);
    const timeLordAddress = await physicalActivityOracle.TIME_LORD();
    const timeLord = new ethers.Contract(timeLordAddress, TimeLordAbi, provider);

    return { physicalActivityOracle, timeLord, physicalActivityOracleAddress, timeLordAddress  };
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
