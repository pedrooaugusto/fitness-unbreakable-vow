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

export interface GymVisitEventProcessed {
    transactionHash: string;
    blockNumber: number;
    weekIndex: number;
    gymLocationLatitudeNanoDegree: bigint;
    gymLocationLongitudeNanoDegree: bigint;
    timestamp: number;
    durationInMinutes: number;
    avgBpm: number;
    maxBpm: number;
}

export interface RunningEventProcessed {
    transactionHash: string;
    blockNumber: number;
    weekIndex: number;
    timestamp: number;
    distanceInMeters: number;
    paceInSecondsPerKm: number;
    avgBpm: number;
}

export interface SleepEventProcessed {
    transactionHash: string;
    blockNumber: number;
    weekIndex: number;
    timestamp: number;
    durationInMinutes: number;
    avgBpm: number;
}

export const EventTopic = {
    PhysicalActivityStatsUpdate: '0x9c8e15dd5df224e4656c6d145fc83045037d5788e2aadf392212aa6ea2a047a4',
    GymVisitEventProcessed:      '0x20f12586551b641b994c32e0cca07fc8199c379919716901058f58c4611b76a3',
    RunningEventProcessed:       '0xcd40730d474c4d9964ec35dfa41505008a963009b1b6bd55e463b55c27c8521e',
    SleepEventProcessed:         '0x036b943bd7cab3bfd6d0da1fffeb3d73a0de9f3ea7826e9dff1024544b68a72c'
}

const PhysicalActivityOracleInterface = new Interface(PhysicalActivityOracleAbi);

export async function loadContracts(physicalActivityOracleAddress?: string, network?: string) {
    if (physicalActivityOracleAddress == null || network == null) throw new Error('Contract address and network must be provided.');

    const rpcUrl = requireEnv(network + '_RPC_PROVIDER');
    const provider = new ethers.JsonRpcProvider(rpcUrl);
    const physicalActivityOracle = new ethers.Contract(physicalActivityOracleAddress, PhysicalActivityOracleAbi, provider);
    const timeLordAddress = await physicalActivityOracle.TIME_LORD();
    const timeLord = new ethers.Contract(timeLordAddress, TimeLordAbi, provider);

    return { physicalActivityOracle, timeLord, physicalActivityOracleAddress, timeLordAddress, network };
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

export function parseGymVisitEventProcessed(etherScanLogs: LogEntry[]) {
    const decoded: GymVisitEventProcessed[] = [];

    for (const logEntry of etherScanLogs) {
        try {
            const parsed = PhysicalActivityOracleInterface.parseLog({ topics: logEntry.topics, data: logEntry.data });

            decoded.push({
                weekIndex: Number(parsed!.args.weekIndex),
                gymLocationLatitudeNanoDegree: parsed!.args.gymLocationLatitudeNanoDegree,
                gymLocationLongitudeNanoDegree: parsed!.args.gymLocationLongitudeNanoDegree,
                timestamp: Number(parsed!.args.timestamp),
                durationInMinutes: Number(parsed!.args.durationInMinutes),
                avgBpm: Number(parsed!.args.avgBpm),
                maxBpm: Number(parsed!.args.maxBpm),
                transactionHash: logEntry.transactionHash,
                blockNumber: Number(logEntry.blockNumber)
            });
        } catch (err) {
            console.error('Unable to parse GymVisitEventProcessed event: ', err);
            throw err;
        }
    }

    return decoded;
}

export function parseRunningEventProcessed(etherScanLogs: LogEntry[]) {
    const decoded: RunningEventProcessed[] = [];

    for (const logEntry of etherScanLogs) {
        try {
            const parsed = PhysicalActivityOracleInterface.parseLog({ topics: logEntry.topics, data: logEntry.data });

            decoded.push({
                weekIndex: Number(parsed!.args.weekIndex),
                timestamp: Number(parsed!.args.timestamp),
                distanceInMeters: Number(parsed!.args.distanceInMeters),
                paceInSecondsPerKm: Number(parsed!.args.paceInSecondsPerKm),
                avgBpm: Number(parsed!.args.avgBpm),
                transactionHash: logEntry.transactionHash,
                blockNumber: Number(logEntry.blockNumber)
            });
        } catch (err) {
            console.error('Unable to parse RunningEventProcessed event: ', err);
            throw err;
        }
    }

    return decoded;
}

export function parseSleepEventProcessed(etherScanLogs: LogEntry[]) {
    const decoded: SleepEventProcessed[] = [];

    for (const logEntry of etherScanLogs) {
        try {
            const parsed = PhysicalActivityOracleInterface.parseLog({ topics: logEntry.topics, data: logEntry.data });

            decoded.push({
                weekIndex: Number(parsed!.args.weekIndex),
                timestamp: Number(parsed!.args.timestamp),
                durationInMinutes: Number(parsed!.args.durationInMinutes),
                avgBpm: Number(parsed!.args.avgBpm),
                transactionHash: logEntry.transactionHash,
                blockNumber: Number(logEntry.blockNumber)
            });
        } catch (err) {
            console.error('Unable to parse SleepEventProcessed event: ', err);
            throw err;
        }
    }

    return decoded;
}
