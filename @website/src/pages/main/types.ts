export const WeeklyGoalStatus = {
    NULL: 0,
    COMPLETED: 1,
    PENDING_END_OF_WEEK: 2,
    FAILED_PENDING_PENALTY: 3,
    FAILED_PENALTY_APPLIED: 4,
} as const;

export type WeeklyGoalStatusType = typeof WeeklyGoalStatus[keyof typeof WeeklyGoalStatus];

export const ContractPhase = {
    ACTIVE: 0,
    GRACE: 1,
    FULLY_EXPIRED: 2,
} as const;

export type ContractPhaseType = typeof ContractPhase[keyof typeof ContractPhase];

export type Network = 'sepolia' | 'arbitrum' | 'localhost';

export interface WeeklyGoal {
    status: WeeklyGoalStatusType;
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
    gracePeriod: number;
    currentDate: number;
    isContractExpired: boolean;
    contractPhase: ContractPhaseType;

    initialStakedAmount: number;
    penaltyAmount: number;
    currentBalance: number;

    secondsInAWeek: number;
    currentWeekNumber: number;
    currentWeekPhysicalActivityRecord: {
        gymVisits: number;
        highestDistanceRanInMeters: number;
        healthySleepNights: number;
    };

    allWeeks: WeeklyGoal[];
    network: Network;

    gymVisitsGoal: number;
    healthySleepNightsGoal: number;
    runDistanceGoal: number;
    requiredNumberOfCompletedGoals: number;
}


export interface GetWeekDetailsResponse {
    weekIndex: number;
    goals: {
        status: number;
        gymVisitsGoalMet: boolean;
        run2KmGoalMet: boolean;
        sleptWellGoalMet: boolean;
        gymVisits: number;
        highestDistanceRanInMeters: number;
        healthySleepNights: number;
    };
    history: ({
        transactionHash: string;
        blockNumber: number;
    } & PhysicalActivityRecordProcessed)[]
}

export type Currency = "usd" | "brl" | "eth";