export enum WeeklyGoalStatus {
    NULL,
    COMPLETED,
    PENDING_END_OF_WEEK,
    FAILED_PENDING_PENALTY,
    FAILED_PENALTY_APPLIED
}

export type Network = 'sepolia' | 'arbitrum' | 'localhost';

export interface WeeklyGoal {
    status: WeeklyGoalStatus;
    goals: {
        gymVisitsGoalMet: boolean;
        run2KmGoalMet: boolean;
        sleptWellGoalMet: boolean;
    },
    penaltyDetails?: PenaltyApplied;
}

export interface ContractEvent {
    transactionHash: string;
}

export interface PhysicalActivityRecordProcessed extends ContractEvent {
    weekIndex: number,
    runDistanceMeters: number,
    gymVisits: number,
    healthySleepNights: number,
}

export interface PenaltyApplied extends ContractEvent {
    weekIndex: number,
    enforcer: string,
    enforcedByUpkeeper: boolean,
    amount: number,
}

export interface GetContractOverviewResponse {
    contractAddress: string;
    oracleAddress: string;

    startDate: number;
    expirationDate: number;
    currentDate: number;

    initialStakedAmount: number;
    penaltyAmount: number;
    currentBalance: number;
    isPenaltyLikely: boolean;

    secondsInAWeek: number;
    currentWeekNumber: number;
    currentWeekMetrics: {
        gymVisits: number;
        highestDistanceRanInMeters: number;
        healthySleepNights: number;
    };

    pastWeeksGoalsResult: WeeklyGoal[];
    isContractExpired: boolean;
    network: Network
}

export interface GetWeekDetailsResponse {
    weekIndex: number;
    goals: {
        status: Number;
        gymVisitsGoalMet: boolean;
        run2KmGoalMet: boolean;
        sleptWellGoalMet: boolean;
        gymVisits: Number;
        highestDistanceRanInMeters: Number;
        healthySleepNights: Number;
    };
    history: ({
        transactionHash: string;
        blockNumber: number;
    } & PhysicalActivityRecordProcessed)[]
}