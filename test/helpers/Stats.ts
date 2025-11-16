import { sign } from '../../scripts/keys';
import crypto from 'crypto';
import { GymVisitEventStruct, GymVisitStatsStruct, RunningEventStruct, RunningStatsStruct, SleepEventStruct, SleepStatsStruct } from '../../typechain-types/contracts/PhysicalActivityOracle';

export const RUNNING_EVENT = 0x11;
export const SLEEP_EVENT = 0x13;
export const GYM_VISIT_EVENT = 0x17;

export const EMPTY_SLEEP_STAT: SleepStatsStruct = { count: 0, longestSleepInMinutes: 0, totalSleepInMinutes: 0 };
export const EMPTY_GYM_VISIT_STAT: GymVisitStatsStruct = { avgBpm: 0, count: 0, totalMinutes: 0 };
export const EMPTY_RUNNING_STAT: RunningStatsStruct = { bestPaceInSecondsPerKm: 0, count: 0, longestDistanceInMeters: 0, maxAvgBpm: 0, totalDistanceInMeters: 0 };

export function mergeRunning(current: RunningStatsStruct, events: RunningEventStruct[]): RunningStatsStruct {
    const result: RunningStatsStruct = {
        count: Number(current.count),
        longestDistanceInMeters: Number(current.longestDistanceInMeters),
        totalDistanceInMeters: Number(current.totalDistanceInMeters),
        bestPaceInSecondsPerKm: Number(current.bestPaceInSecondsPerKm),
        maxAvgBpm: Number(current.maxAvgBpm),
    };

    for (const ev of events) {
        if (!isRunningEventValid(ev)) continue;

        result.count = Number(result.count) + 1;
        result.totalDistanceInMeters = Number(result.totalDistanceInMeters) + Number(ev.distanceInMeters);

        const distance = Number(ev.distanceInMeters);
        if (distance > Number(result.longestDistanceInMeters)) {
            result.longestDistanceInMeters = distance;
        }

        const pace = Number(ev.paceInSecondsPerKm);
        if (Number(result.bestPaceInSecondsPerKm) === 0 || pace < Number(result.bestPaceInSecondsPerKm)) {
            result.bestPaceInSecondsPerKm = pace;
        }

        const bpm = Number(ev.avgBpm);
        if (bpm > Number(result.maxAvgBpm)) {
            result.maxAvgBpm = bpm;
        }
    }

    return result;
}

export async function signRunningEvent(record: Omit<RunningEventStruct, 'signature'> & { useWrongSignature?: boolean }) {
    const buffer = Buffer.alloc(20);

    const eventType = record.useWrongSignature ? 5 : RUNNING_EVENT;

    buffer.writeUInt32BE(eventType >> 0, 0);
    buffer.writeUInt32BE(record.timestamp.valueOf() as number >>> 0, 4);
    buffer.writeUInt32BE(record.distanceInMeters.valueOf() as number >>> 0, 8);
    buffer.writeUInt32BE(record.paceInSecondsPerKm.valueOf() as number >>> 0, 12);
    buffer.writeUInt32BE(record.avgBpm.valueOf() as number >>> 0, 16);

    const signature = await sign(buffer);

    return { ...record, signature: signature };
}

export function isRunningEventValid(evento: RunningEventStruct) {
    return  Number(evento.distanceInMeters) >= 1000 &&
            Number(evento.paceInSecondsPerKm) <= (8 * 60) &&
            Number(evento.avgBpm) >= 110;
}

export async function signSleepEvent(record: Omit<SleepEventStruct, 'signature'> & { useWrongSignature?: boolean }) {
    const buffer = Buffer.alloc(16);

    const eventType = record.useWrongSignature ? 55 : SLEEP_EVENT;

    buffer.writeUInt32BE(eventType >> 0, 0);
    buffer.writeUInt32BE(record.timestamp.valueOf() as number >>> 0, 4);
    buffer.writeUInt32BE(record.durationInMinutes.valueOf() as number >>> 0, 8);
    buffer.writeUInt32BE(record.avgBpm.valueOf() as number >>> 0, 12);

    const signature = await sign(buffer);

    return { ...record, signature: signature };
}

export function mergeSleep(current: SleepStatsStruct, events: SleepEventStruct[]): SleepStatsStruct {
    const result: SleepStatsStruct = {
        count: Number(current.count),
        longestSleepInMinutes: Number(current.longestSleepInMinutes),
        totalSleepInMinutes: Number(current.totalSleepInMinutes),
    };

    for (const ev of events) {
        if (!isSleepEventValid(ev)) continue;

        result.count = Number(result.count) + 1;
        result.totalSleepInMinutes = Number(result.totalSleepInMinutes) + Number(ev.durationInMinutes);

        const total = Number(ev.durationInMinutes);
        if (total > Number(result.longestSleepInMinutes)) {
            result.longestSleepInMinutes = total;
        }
    }

    return result;
}

export function isSleepEventValid(evento: SleepEventStruct) {
    return  Number(evento.durationInMinutes) >= (6 * 60 + 10) &&
            Number(evento.avgBpm) >= 50 && Number(evento.avgBpm) <= 80;
}

export async function signGymVisitEvent(record: Omit<GymVisitEventStruct, 'signature'> & { useWrongSignature?: boolean }) {
    const buffer = Buffer.alloc(36);

    const eventType = record.useWrongSignature ? 555 : GYM_VISIT_EVENT;

    buffer.writeUInt32BE(eventType >>> 0, 0);
    buffer.writeBigInt64BE(record.location.latitudeNanoDegree.valueOf() as bigint, 4);
    buffer.writeBigInt64BE(record.location.longitudeNanoDegree.valueOf() as bigint, 12);
    buffer.writeUInt32BE((record.timestamp.valueOf() as number >>> 0), 20);
    buffer.writeUInt32BE((record.durationInMinutes.valueOf() as number >>> 0), 24);
    buffer.writeUInt32BE((record.avgBpm.valueOf() as number >>> 0), 28);
    buffer.writeUInt32BE((record.maxBpm.valueOf() as number >>> 0), 32);

    const signature = await sign(buffer);

    return { ...record, signature: signature };
}

export function mergeGymVisit(current: GymVisitStatsStruct, events: GymVisitEventStruct[]): GymVisitStatsStruct {
    const result: GymVisitStatsStruct = {
        count: Number(current.count),
        totalMinutes: Number(current.totalMinutes),
        avgBpm: Number(current.avgBpm)
    };

    for (const ev of events) {
        if (!isGymVisitEventValid(ev)) continue;

        const oldCount = Number(result.count);

        result.count = oldCount + 1;

        result.totalMinutes = Number(result.totalMinutes) + Number(ev.durationInMinutes);

        if (oldCount == 0) {
            result.avgBpm = ev.avgBpm;
        } else {
            const weightedSum = Number(result.avgBpm) * Number(oldCount) + Number(ev.avgBpm);

            const newAvg = Math.floor(weightedSum / (oldCount + 1));

            result.avgBpm = newAvg;
        }
    }

    return result;
}

export function isGymVisitEventValid(evento: GymVisitEventStruct) {
    const validLocation = 
        isSameLocation(Number(evento.location.latitudeNanoDegree), Number(evento.location.longitudeNanoDegree), 0, 0) ||
        isSameLocation(Number(evento.location.latitudeNanoDegree), Number(evento.location.longitudeNanoDegree), -228969577, -432726589);

    return  validLocation &&
            Number(evento.durationInMinutes) >= 10 &&
            Number(evento.avgBpm) >= 95;
}

export function createSHA256Hash(inputString: string) {
    return crypto.createHash('sha256').update(inputString).digest('hex');
}

export function isSameLocation(x1: number, y1: number, x2: number, y2: number) {
    return x1 == x2 && y1 == y2;
}