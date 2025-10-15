import type { GetWeekDetailsResponse, PhysicalActivityRecordProcessed } from "../types";
import loadContract from "./load-contract";

export async function getWeekDetails(weekIndex: string): Promise<GetWeekDetailsResponse> {
    const {
        getEvents,
        PhysicalActivityOracle,
        FitnessUnbreakableVow,
    } = await loadContract();

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