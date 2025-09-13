import { APIGatewayProxyEvent, APIGatewayProxyResult } from "aws-lambda";
import { FitnessUnbreakableVow, getEvents, PhysicalActivityOracle } from "./contracts";
import { GetWeekDetailsResponse, PhysicalActivityRecordProcessed, WeeklyGoal } from "./types";

export const handler = async (event: APIGatewayProxyEvent): Promise<APIGatewayProxyResult> => {
    try {
        if (event.pathParameters?.weekIndex == undefined) throw new Error('Week Index not provided.');
 
        const overview = await getWeekDetails(event.pathParameters.weekIndex);

        return { statusCode: 200, body: JSON.stringify(overview) };
    } catch (e) {
        console.error(e);

        return {
            statusCode: 500,
            body: JSON.stringify({ errorMessage: 'Error trying to load week details.\nError: ' + (e || '').toString()})
        }
    }
};

export async function getWeekDetails(weekIndex: string): Promise<GetWeekDetailsResponse> {
    const weekGoals = await FitnessUnbreakableVow.weeklyGoalsRecords!(weekIndex);
    const weekMetrics = await PhysicalActivityOracle.physicalActivityRecords!(weekIndex);
    const recordsAddedInWeek = await getEvents<PhysicalActivityRecordProcessed>(
        FitnessUnbreakableVow,
        'PhysicalActivityRecordProcessed',
        [weekIndex],
        ['weekIndex', 'runDistanceMeters', 'gymVisits', 'healthySleepNights']
    ) || [];

    const goals = {
        status: Number(weekGoals.status),
        gymVisitsGoalMet: weekGoals.wentoToTheGymEnoughTimes,
        run2KmGoalMet: weekGoals.ran2km,
        sleptWellGoalMet: weekGoals.sleptWell,
        gymVisits: Number(weekMetrics.gymVisits),
        highestDistanceRanInMeters: Number(weekMetrics.runDistanceMeters),
        healthySleepNights: Number(weekMetrics.healthySleepNights),
    };

    return {
        weekIndex: Number(weekIndex),
        goals: goals,
        history: recordsAddedInWeek
    };
}