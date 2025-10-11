import { APIGatewayProxyEvent, APIGatewayProxyResult } from 'aws-lambda'
import { PhysicalActivityOracle, FitnessUnbreakableVow, FitnessUnbreakableVowAddress, executeMulticall, getBalance, getEvents, NETWORK } from './contracts';
import { GetContractOverviewResponse, PenaltyApplied, WeeklyGoal, WeeklyGoalStatus } from './types';
import { ethers } from 'ethers';

export const handler = async (event: APIGatewayProxyEvent): Promise<APIGatewayProxyResult> => {
    try {
        const overview = await getContractOverview();

        return { statusCode: 200, body: JSON.stringify(overview) };
    } catch (e) {
        console.error(e);

        return {
            statusCode: 500,
            body: JSON.stringify({ errorMessage: 'Error trying to load contract current state.\nError: ' + (e || '').toString()})
        }
    }
};

export async function getContractOverview(): Promise<GetContractOverviewResponse> {
    const contractBalance = await getBalance(FitnessUnbreakableVow);

    const [
        oracleAddress,
        startDate,
        expirationDate,
        initialStakedAmount,
        penaltyAmount,
        currentWeekNumber,
        pastWeeksGoalsRaw,
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

    const [[, currentWeekRecord], secondsInAWeek] = await executeMulticall(PhysicalActivityOracle, [
        'getCurrentWeekPhysicalActivityRecord',
        'SECONDS_IN_ONE_WEEK'
    ]);

    const currentWeekMetrics = {
        gymVisits: Number(currentWeekRecord.gymVisits),
        highestDistanceRanInMeters: Number(currentWeekRecord.runDistanceMeters),
        healthySleepNights: Number(currentWeekRecord.healthySleepNights),
    };

    const penaltyAppliedEvents = (await getEvents<PenaltyApplied>(FitnessUnbreakableVow, 'PenaltyApplied', [], ['weekIndex', 'enforcer']) || [])
        .map(penaltyApplied => ({
            ...penaltyApplied,
            amount: Number(ethers.formatEther(penaltyAmount)),
            enforcedByUpkeeper: penaltyApplied.enforcer === upkeeperAddress
        }));

    const pastWeeksGoalsResult: WeeklyGoal[] = pastWeeksGoalsRaw.map((r: any, index: number) => ({
        status: Number(r.status),// == 0 ? 3 : Number(r.status) as WeeklyGoalStatus,
        goals: {
            gymVisitsGoalMet: r.wentoToTheGymEnoughTimes,
            run2KmGoalMet: r.ran2km,
            sleptWellGoalMet: r.sleptWell,
        },
        penaltyDetails: penaltyAppliedEvents.find(item => item.weekIndex === index)
    }));

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
        currentWeekMetrics,
        pastWeeksGoalsResult,
        isContractExpired: Number(contractPhase) != 0,
        contractPhase: Number(contractPhase),
        gracePeriod: Number(gracePeriod),
        secondsInAWeek: Number(secondsInAWeek),
        network: NETWORK,
        gymVisitsGoal: Number(gymVisitsGoal),
        runDistanceGoal: Number(runDistanceGoal),
        healthySleepNightsGoal: Number(healthySleepNightsGoal),
        requiredNumberOfCompletedGoals: Number(requiredNumberOfCompletedGoals)
    };
}