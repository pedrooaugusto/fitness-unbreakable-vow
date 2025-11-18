export const WeeklyGoalStatus = {
    NULL: 0,
    COMPLETED: 1,
    PENDING_END_OF_WEEK: 2,
    FAILED_PENDING_PENALTY: 3,
    FAILED_PENALTY_APPLIED_BY_UPKEEPER: 4,
    FAILED_PENALTY_APPLIED_BY_UNKOWN: 5,
} as const;

export type WeeklyGoalStatusType = typeof WeeklyGoalStatus[keyof typeof WeeklyGoalStatus];

export const ContractPhase = {
    ACTIVE: 0,
    GRACE: 1,
    FULLY_EXPIRED: 2,
} as const;

export type ContractPhaseType = typeof ContractPhase[keyof typeof ContractPhase];

export type Network = 'sepolia' | 'arbitrum' | 'localhost' | 'arbiSep';

export type RunningStats = {
    count: bigint;
    longestDistanceInMeters: bigint;
    totalDistanceInMeters: bigint;
    bestPaceInSecondsPerKm: bigint;
    maxAvgBpm: bigint;
}

export type GymVisitStats = {
    count: bigint;
    totalMinutes: bigint;
    avgBpm: bigint;
}

export type SleepStats = {
    count: bigint;
    longestSleepInMinutes: bigint;
    totalSleepInMinutes: bigint;
}

export interface PhysicalActivityStats {
    timestamp: bigint;
    running: RunningStats;
    gym: GymVisitStats;
    sleep: SleepStats;
}

export interface RunningEventValidator {
    minimumDistanceInMeters: bigint,
    maximumPaceInSecondsPerKm: bigint;
    minimumAvgBpm: bigint;
}

export interface Geofence {
    latitudeNanoDegree: bigint;
    longitudeNanoDegree: bigint;
    radiusInMeters: bigint;
}

export interface GymVisitEventValidator {
    gym1Location: Geofence;
    gym2Location: Geofence;
    gym3Location: Geofence;
    minimumVisitTimeInMinutes: bigint;
    minimumAvgBpm: bigint;
}

export interface SleepEventValidator {
    minimumDurationInMinutes: bigint;
    avgBpmLowerBand: bigint;
    avgBpmUpperBand: bigint;
}

export interface WeeklyGoal {
    status: WeeklyGoalStatusType;
    goals: {
        gymVisitsGoalMet: boolean;
        run2KmGoalMet: boolean;
        sleptWellGoalMet: boolean;
    },
    penaltyBlock?: string;
}

export interface ContractEvent {
    transactionHash: string;
    blockNumber: number;
}

export interface PhysicalActivityRecordProcessed extends ContractEvent {
    weekIndex: number,
    runDistanceMeters: number,
    gymVisits: number,
    healthySleepNights: number,
}

export interface GymVisitEventProcessed extends ContractEvent {
    weekIndex: number;
    location: number;
    timestamp: number;
    durationInMinutes: number;
    avgBpm: number;
    maxBpm: number;
}

export interface RunningEventProcessed extends ContractEvent {
    weekIndex: number;
    timestamp: number;
    distanceInMeters: number;
    paceInSecondsPerKm: number;
    avgBpm: number;
}

export interface SleepEventProcessed extends ContractEvent {
    weekIndex: number;
    timestamp: number;
    durationInMinutes: number;
    avgBpm: number;
}

export interface PenaltyApplied extends ContractEvent {
    weekIndex: number,
    enforcer: string,
}

export interface PhysicalActivityStatsUpdate extends ContractEvent {
    weekIndex: number;
    stats: PhysicalActivityStats;
}

export interface GetContractOverviewResponse {
    contractAddress: string;
    oracleAddress: string;
    upkeeperAddress: string;
    upkeeperId: string;
    upkeeperCronSpec: string;

    version: string,
    startDate: number;
    expirationDate: number;
    gracePeriod: number;
    currentDate: number;
    isContractExpired: boolean;
    contractPhase: ContractPhaseType;

    initialStakedAmount: number;
    penaltyAmount: number;
    currentBalance: number;

    secondsInAWeek: number;
    currentWeekNumber: number;
    currentWeekPhysicalActivityStats: PhysicalActivityStats;

    allWeeks: WeeklyGoal[];
    network: Network;

    gymVisitsGoal: number;
    healthySleepNightsGoal: number;
    runningSessionsGoal: number;
    requiredNumberOfCompletedGoals: number;

    gymVisitValidator: GymVisitEventValidator;
    runningValidator: RunningEventValidator;
    sleepValidator: SleepEventValidator;

    publicKeyInfo: {
        x: string,
        y: string,
        attestation: {
            cidFile: string;
            sha256: string;
            challenge: string;
        }
    }
}


export interface GetWeekDetailsResponse {
    weekIndex: number;
    goals: {
        status: number;
        gymVisitsGoalMet: boolean;
        run2KmGoalMet: boolean;
        sleptWellGoalMet: boolean;
        gymVisits: number;
        runningSessions: number;
        healthySleepNights: number;
    };
    history: PhysicalActivityStatsUpdate[] | null;
    penalty: PenaltyApplied | null;
}

export type Currency = "usd" | "brl" | "eth";