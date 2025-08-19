import { APIGatewayProxyEvent, APIGatewayProxyResult } from 'aws-lambda'
import { PhysicalActivityOracle, FitnessUnbreakableVow, FitnessUnbreakableVowAddress, executeMulticall, getBalance } from './contracts';
import { GetContractOverviewResponse, WeeklyGoalResult, WeeklyGoalStatus } from './types';
import { ethers } from 'ethers';

export const handler = async (event: APIGatewayProxyEvent): Promise<any/*APIGatewayProxyResult*/> => {
    try {
        const overview = await getContractOverview();

        return { statusCode: 200, body: overview };
    } catch (e) {
        console.error(e);

        return { statusCode: 500, message: 'Error trying to load contract current state.'}
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
        isContractExpired,
    ] = await executeMulticall(FitnessUnbreakableVow, [
        "PHYSICAL_ACTIVITY_ORACLE",
        "CREATION_DATE",
        "EXPIRATION_DATE",
        "STAKED_AMOUNT",
        "PENALTY_AMOUNT",
        "getCurrentWeekIndex",
        "getAllWeeklyGoalsRecords",
        "isContractExpired"
    ]);

    const [[, currentWeekRecord], secondsInAWeek] = await executeMulticall(PhysicalActivityOracle, [
        'getCurrentWeekPhysicalActivityRecord',
        'SECONDS_IN_A_WEEK'
    ]);

    const currentWeekMetrics = {
        gymVisits: Number(currentWeekRecord.gymVisits),
        highestDistanceRanInMeters: Number(currentWeekRecord.runDistanceMeters),
        healthySleepNights: Number(currentWeekRecord.healthySleepNights),
    };

    const pastWeeksGoalsResult: WeeklyGoalResult[] = pastWeeksGoalsRaw.map((r: any) => ({
        status: Number(r.status) as WeeklyGoalStatus,
        gymVisitsGoalMet: r.wentoToTheGymEnoughTimes,
        run2KmGoalMet: r.ran2km,
        sleptWellGoalMet: r.sleptWell,
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
        isPenaltyLikely: false,
        currentWeekNumber: Number(currentWeekNumber),
        currentWeekMetrics,
        pastWeeksGoalsResult,
        isContractExpired,
        secondsInAWeek: Number(secondsInAWeek)
    };
}