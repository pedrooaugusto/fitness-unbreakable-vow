import { Contract } from 'ethers';
import { P256Signature, signP256 } from './p256';

const RUNNING_EVENT = 0x11;
const SLEEP_EVENT = 0x13;
const GYM_VISIT_EVENT = 0x17;

export type Location = { latitudeNanoDegree: bigint; longitudeNanoDegree: bigint };

export type RunningEvent = {
    signature: P256Signature;
    timestamp: number;
    distanceInMeters: number;
    paceInSecondsPerKm: number;
    avgBpm: number;
};

export type SleepEvent = {
    signature: P256Signature;
    timestamp: number;
    durationInMinutes: number;
    avgBpm: number;
};

export type GymVisitEvent = {
    signature: P256Signature;
    location: Location;
    timestamp: number;
    durationInMinutes: number;
    avgBpm: number;
    maxBpm: number;
};

export type PhysicalActivityRequest = {
    running: RunningEvent[];
    sleep: SleepEvent[];
    gymVisit: GymVisitEvent[];
};

export type RunningEventValidator = {
    minimumDistanceInMeters: number;
    maximumPaceInSecondsPerKm: number;
    minimumAvgBpm: number;
};

export type SleepEventValidator = {
    minimumDurationInMinutes: number;
    avgBpmLowerBand: number;
    avgBpmUpperBand: number;
};

export type GymVisitValidator = {
    gymLocations: Location[];
    minimumVisitTimeInMinutes: number;
    minimumAvgBpm: number;
    minimumMaxBpm: number;
};

export type ActivityValidators = {
    running: RunningEventValidator;
    sleep: SleepEventValidator;
    gymVisit: GymVisitValidator;
};

export async function createRandomPhysicalActivityRequest(params: {
    countPerType: number;
    weekStart: number;
    secondsInWeek: number;
    physicalActivityOracle: Contract;
    invalidRate?: number;
}): Promise<PhysicalActivityRequest> {
    const { countPerType, weekStart, secondsInWeek } = params;

    const validators = await fetchActivityValidators(params.physicalActivityOracle);
    const invalidRate = normalizeRate(params.invalidRate);

    const running = await Promise.all(
        Array.from({ length: randomBetween(1, 3) }, () => createRunningEvent(weekStart, secondsInWeek, validators.running, invalidRate))
    );

    const sleep = await Promise.all(
        Array.from({ length: randomBetween(0, 4) }, () => createSleepEvent(weekStart, secondsInWeek, validators.sleep, invalidRate))
    );

    const gymVisit = await Promise.all(
        Array.from({ length: randomBetween(0, 5) }, () => createGymVisitEvent(weekStart, secondsInWeek, validators.gymVisit, invalidRate))
    );

    return { running, sleep, gymVisit };
}

async function createRunningEvent(
    weekStart: number,
    secondsInWeek: number,
    validator: RunningEventValidator,
    invalidRate: number
): Promise<RunningEvent> {
    const distanceMin = validator.minimumDistanceInMeters;
    const distanceMax = Math.min(distanceMin + 3000, 60000);
    const paceMax = validator.maximumPaceInSecondsPerKm;
    const paceMin = Math.max(60, paceMax - 200);
    const bpmMin = validator.minimumAvgBpm;
    const bpmMax = Math.min(bpmMin + 40, 200);

    const record = {
        timestamp: randomTimestamp(weekStart, secondsInWeek),
        distanceInMeters: randomBetween(distanceMin, distanceMax),
        paceInSecondsPerKm: randomBetween(paceMin, paceMax),
        avgBpm: randomBetween(bpmMin, bpmMax),
    };

    if (shouldGenerateInvalid(invalidRate)) {
        applyRunningInvalidation(record, validator);
    }

    return { ...record, signature: await signRunningEvent(record) };
}

async function createSleepEvent(
    weekStart: number,
    secondsInWeek: number,
    validator: SleepEventValidator,
    invalidRate: number
): Promise<SleepEvent> {
    const durationMin = validator.minimumDurationInMinutes;
    const durationMax = Math.min(durationMin + 240, 12 * 60);
    const bpmMin = validator.avgBpmLowerBand;
    const bpmMax = validator.avgBpmUpperBand;

    const record = {
        timestamp: randomTimestamp(weekStart, secondsInWeek),
        durationInMinutes: randomBetween(durationMin, durationMax),
        avgBpm: randomBetween(bpmMin, bpmMax),
    };

    if (shouldGenerateInvalid(invalidRate)) {
        applySleepInvalidation(record, validator);
    }

    return { ...record, signature: await signSleepEvent(record) };
}

async function createGymVisitEvent(
    weekStart: number,
    secondsInWeek: number,
    validator: GymVisitValidator,
    invalidRate: number
): Promise<GymVisitEvent> {
    if (validator.gymLocations.length === 0) {
        throw new Error('No gym locations available to generate gym visits.');
    }

    const durationMin = validator.minimumVisitTimeInMinutes;
    const durationMax = Math.min(durationMin + 40, 120);
    const avgBpmMin = validator.minimumAvgBpm;
    const avgBpmMax = Math.min(avgBpmMin + 50, 200);
    const maxBpmMin = validator.minimumMaxBpm;
    const maxBpmMax = Math.min(maxBpmMin + 50, 220);

    const record = {
        location: randomFromArray(validator.gymLocations),
        timestamp: randomTimestamp(weekStart, secondsInWeek),
        durationInMinutes: randomBetween(durationMin, durationMax),
        avgBpm: randomBetween(avgBpmMin, avgBpmMax),
        maxBpm: randomBetween(maxBpmMin, maxBpmMax),
    };

    if (shouldGenerateInvalid(invalidRate)) {
        applyGymInvalidation(record, validator);
    }

    return { ...record, signature: await signGymVisitEvent(record) };
}

async function signRunningEvent(record: Omit<RunningEvent, 'signature'>): Promise<P256Signature> {
    const buffer = Buffer.alloc(20);

    buffer.writeUInt32BE(RUNNING_EVENT >>> 0, 0);
    buffer.writeUInt32BE(record.timestamp >>> 0, 4);
    buffer.writeUInt32BE(record.distanceInMeters >>> 0, 8);
    buffer.writeUInt32BE(record.paceInSecondsPerKm >>> 0, 12);
    buffer.writeUInt32BE(record.avgBpm >>> 0, 16);

    return await signP256(buffer);
}

async function signSleepEvent(record: Omit<SleepEvent, 'signature'>): Promise<P256Signature> {
    const buffer = Buffer.alloc(16);

    buffer.writeUInt32BE(SLEEP_EVENT >>> 0, 0);
    buffer.writeUInt32BE(record.timestamp >>> 0, 4);
    buffer.writeUInt32BE(record.durationInMinutes >>> 0, 8);
    buffer.writeUInt32BE(record.avgBpm >>> 0, 12);

    return await signP256(buffer);
}

async function signGymVisitEvent(record: Omit<GymVisitEvent, 'signature'>): Promise<P256Signature> {
    const buffer = Buffer.alloc(36);

    buffer.writeUInt32BE(GYM_VISIT_EVENT >>> 0, 0);
    buffer.writeBigInt64BE(record.location.latitudeNanoDegree, 4);
    buffer.writeBigInt64BE(record.location.longitudeNanoDegree, 12);
    buffer.writeUInt32BE(record.timestamp >>> 0, 20);
    buffer.writeUInt32BE(record.durationInMinutes >>> 0, 24);
    buffer.writeUInt32BE(record.avgBpm >>> 0, 28);
    buffer.writeUInt32BE(record.maxBpm >>> 0, 32);

    return await signP256(buffer);
}

async function fetchActivityValidators(oracle: Contract): Promise<ActivityValidators> {
    const [running, sleep, gymVisit] = await Promise.all([
        oracle.runningValidator(),
        oracle.sleepValidator(),
        oracle.gymVisitValidator(),
    ]);

    const gymLocations = [
        gymVisit.gymLoc1,
        gymVisit.gymLoc2,
        gymVisit.gymLoc3,
        gymVisit.gymLoc4,
    ].map((loc: any) => ({
        latitudeNanoDegree: toBigInt(loc.latitudeNanoDegree),
        longitudeNanoDegree: toBigInt(loc.longitudeNanoDegree),
    }));

    return {
        running: {
            minimumDistanceInMeters: Number(running.minimumDistanceInMeters),
            maximumPaceInSecondsPerKm: Number(running.maximumPaceInSecondsPerKm),
            minimumAvgBpm: Number(running.minimumAvgBpm),
        },
        sleep: {
            minimumDurationInMinutes: Number(sleep.minimumDurationInMinutes),
            avgBpmLowerBand: Number(sleep.avgBpmLowerBand),
            avgBpmUpperBand: Number(sleep.avgBpmUpperBand),
        },
        gymVisit: {
            gymLocations,
            minimumVisitTimeInMinutes: Number(gymVisit.minimumVisitTimeInMinutes),
            minimumAvgBpm: Number(gymVisit.minimumAvgBpm),
            minimumMaxBpm: Number(gymVisit.minimumMaxBpm),
        },
    };
}

function randomTimestamp(weekStart: number, secondsInWeek: number) {
    const maxOffset = Math.max(0, secondsInWeek - 1);

    return weekStart + randomBetween(0, maxOffset);
}

function randomBetween(min: number, max: number) {
    if (max <= min) return min;

    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomFromArray<T>(items: T[]) {
    return items[randomBetween(0, items.length - 1)];
}

function normalizeRate(rate: number | undefined) {
    if (rate == null || !Number.isFinite(rate)) return 0.15;
    if (rate <= 0) return 0;

    const normalized = rate > 1 ? rate / 100 : rate;

    return Math.min(Math.max(normalized, 0), 1);
}

function shouldGenerateInvalid(invalidRate: number) {
    if (invalidRate <= 0) return false;

    return Math.random() < invalidRate;
}

function applyRunningInvalidation(
    record: Omit<RunningEvent, 'signature'>,
    validator: RunningEventValidator
) {
    const invalidations: Array<() => void> = [];

    if (validator.minimumDistanceInMeters > 1) {
        invalidations.push(() => {
            record.distanceInMeters = randomBelow(validator.minimumDistanceInMeters, 1);
        });
    }

    if (validator.maximumPaceInSecondsPerKm < 65535) {
        invalidations.push(() => {
            record.paceInSecondsPerKm = randomAbove(validator.maximumPaceInSecondsPerKm, 120, 65535);
        });
    }

    if (validator.minimumAvgBpm > 1) {
        invalidations.push(() => {
            record.avgBpm = randomBelow(validator.minimumAvgBpm, 1);
        });
    }

    if (invalidations.length === 0) return;

    randomFromArray(invalidations)();
}

function applySleepInvalidation(
    record: Omit<SleepEvent, 'signature'>,
    validator: SleepEventValidator
) {
    const invalidations: Array<() => void> = [];

    if (validator.minimumDurationInMinutes > 1) {
        invalidations.push(() => {
            record.durationInMinutes = randomBelow(validator.minimumDurationInMinutes, 1);
        });
    }

    if (validator.avgBpmLowerBand > 1) {
        invalidations.push(() => {
            record.avgBpm = randomBelow(validator.avgBpmLowerBand, 1);
        });
    }

    if (validator.avgBpmUpperBand < 255) {
        invalidations.push(() => {
            record.avgBpm = randomAbove(validator.avgBpmUpperBand, 10, 255);
        });
    }

    if (invalidations.length === 0) return;

    randomFromArray(invalidations)();
}

function applyGymInvalidation(
    record: Omit<GymVisitEvent, 'signature'>,
    validator: GymVisitValidator
) {
    const invalidations: Array<() => void> = [];

    if (validator.gymLocations.length > 0) {
        invalidations.push(() => {
            record.location = randomInvalidLocation(validator.gymLocations);
        });
    }

    if (validator.minimumVisitTimeInMinutes > 1) {
        invalidations.push(() => {
            record.durationInMinutes = randomBelow(validator.minimumVisitTimeInMinutes, 1);
        });
    }

    if (validator.minimumAvgBpm > 1) {
        invalidations.push(() => {
            record.avgBpm = randomBelow(validator.minimumAvgBpm, 1);
        });
    }

    if (validator.minimumMaxBpm > 1) {
        invalidations.push(() => {
            record.maxBpm = randomBelow(validator.minimumMaxBpm, 1);
        });
    }

    if (invalidations.length === 0) return;

    randomFromArray(invalidations)();
}

function randomInvalidLocation(locations: Location[]) {
    const base = randomFromArray(locations);

    return {
        latitudeNanoDegree: base.latitudeNanoDegree + 1n,
        longitudeNanoDegree: base.longitudeNanoDegree,
    };
}

function randomBelow(minValue: number, floor: number) {
    const max = Math.max(floor, minValue - 1);

    return randomBetween(floor, max);
}

function randomAbove(maxValue: number, spread: number, cap: number) {
    const start = Math.min(maxValue + 1, cap);
    const end = Math.min(maxValue + Math.max(1, spread), cap);

    if (end < start) return start;

    return randomBetween(start, end);
}

function toBigInt(value: unknown) {
    if (typeof value === 'bigint') return value;
    if (typeof value === 'number') return BigInt(value);
    if (typeof value === 'string') return BigInt(value);

    throw new Error(`Unsupported bigint value: ${String(value)}`);
}
