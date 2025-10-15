import { ethers } from 'ethers';
import type { ContractPhaseType, GetContractOverviewResponse, PenaltyApplied, WeeklyGoal } from '../types';
import loadContract from './load-contract';

export async function getContractOverview(): Promise<GetContractOverviewResponse> {
    const {
        getBalance,
        executeMulticall,
        getEvents,
        network,
        PhysicalActivityOracle,
        FitnessUnbreakableVow,
        FitnessUnbreakableVowAddress,
    } = await loadContract();
 
    const contractBalance = await getBalance(FitnessUnbreakableVow);

    const [
        oracleAddress,
        startDate,
        expirationDate,
        initialStakedAmount,
        penaltyAmount,
        currentWeekNumber,
        allWeeksRaw,
        contractPhase,
        gracePeriod,
        upkeeperAddress,
        gymVisitsGoal,
        healthySleepNightsGoal,
        runDistanceGoal,
        requiredNumberOfCompletedGoals,
    ] = await executeMulticall(FitnessUnbreakableVow, [
        "PHYSICAL_ACTIVITY_ORACLE",
        "CREATION_DATE",
        "EXPIRATION_DATE",
        "STAKED_AMOUNT",
        "PENALTY_AMOUNT",
        "getCurrentWeekIndex",
        "getAllWeeklyGoalsRecords",
        "getContractPhase",
        "GRACE_PERIOD",
        "CHAINLINK_UPKEEP_ADDRESS",
        "GYM_VISITS_GOAL",
        "HEALTHY_SLEEP_NIGHTS_GOAL",
        "RUN_DISTANCE_GOAL",
        "REQUIRED_NUMBER_OF_COMPLETED_GOALS"
    ]);

    const [
        [, currentWeekPhysicalActivityRecordRaw],
        secondsInAWeek
    ] = await executeMulticall(PhysicalActivityOracle, [
        'getCurrentWeekPhysicalActivityRecord',
        'SECONDS_IN_ONE_WEEK'
    ]);

    const currentWeekPhysicalActivityRecord = {
        gymVisits: Number(currentWeekPhysicalActivityRecordRaw.gymVisits),
        highestDistanceRanInMeters: Number(currentWeekPhysicalActivityRecordRaw.runDistanceMeters),
        healthySleepNights: Number(currentWeekPhysicalActivityRecordRaw.healthySleepNights),
    };

    const penaltyAppliedEvents = await getEvents<PenaltyApplied>(FitnessUnbreakableVow, 'PenaltyApplied', [], ['weekIndex', 'enforcer']);

    const penaltiesApplied = penaltyAppliedEvents.map(item => enrichPenaltyDetails(item, penaltyAmount, upkeeperAddress));

    const allWeeks = allWeeksRaw.map((weeklyGoal: any, index: number) => enrichWeeklyGoal(weeklyGoal, index, penaltiesApplied)) as WeeklyGoal[];

    return {
        contractAddress: FitnessUnbreakableVowAddress,
        oracleAddress,
        startDate: Number(startDate),
        expirationDate: Number(expirationDate),
        currentDate: Math.floor(Date.now() / 1000),
        initialStakedAmount: Number(ethers.formatEther(initialStakedAmount)),
        penaltyAmount: Number(ethers.formatEther(penaltyAmount)),
        currentBalance: Number(ethers.formatEther(contractBalance)),
        currentWeekNumber: Number(currentWeekNumber),
        currentWeekPhysicalActivityRecord,
        allWeeks,
        isContractExpired: Number(contractPhase) != 0,
        contractPhase: Number(contractPhase) as ContractPhaseType,
        gracePeriod: Number(gracePeriod),
        secondsInAWeek: Number(secondsInAWeek),
        network: network,
        gymVisitsGoal: Number(gymVisitsGoal),
        runDistanceGoal: Number(runDistanceGoal),
        healthySleepNightsGoal: Number(healthySleepNightsGoal),
        requiredNumberOfCompletedGoals: Number(requiredNumberOfCompletedGoals)
    };
}

function enrichWeeklyGoal(weeklyGoal: Record<string, unknown>, index: number, penaltiesAppliedList: PenaltyApplied[]) {
    return {
        status: Number(weeklyGoal.status),
        goals: {
            gymVisitsGoalMet: weeklyGoal.wentoToTheGymEnoughTimes,
            run2KmGoalMet: weeklyGoal.ran2km,
            sleptWellGoalMet: weeklyGoal.sleptWell,
        },
        penaltyDetails: penaltiesAppliedList.find(item => item.weekIndex === index)
    }
}

function enrichPenaltyDetails(penaltyDetails: PenaltyApplied, penaltyAmount: bigint, upkeeperAddress: string): PenaltyApplied {
    return {
        ...penaltyDetails,
        amount: Number(ethers.formatEther(penaltyAmount)),
        enforcedByUpkeeper: penaltyDetails.enforcer === upkeeperAddress
    }
}
