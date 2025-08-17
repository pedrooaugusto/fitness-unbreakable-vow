export enum WeeklyGoalStatus {
    UNKNOWN,
    COMPLETED,
    PENDING_END_OF_WEEK,
    FAILED_PENDING_PENALTY,
    FAILED_PENALTY_APPLIED
}

export interface WeeklyGoalResult {
    status: WeeklyGoalStatus;
    gymVisitsGoalMet: boolean;
    run2KmGoalMet: boolean;
    sleptWellGoalMet: boolean;
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

    pastWeeksGoalsResult: WeeklyGoalResult[];
    isContractExpired: boolean;
}