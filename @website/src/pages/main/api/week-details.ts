import type { GetWeekDetailsResponse, PenaltyApplied, PhysicalActivityRecordProcessed } from "../types";
import loadContract, { type EnhancedContract } from "./load-contract";

export async function getWeekDetails(weekIndex: string): Promise<GetWeekDetailsResponse> {
    const { PhysicalActivityOracle, FitnessUnbreakableVow } = await loadContract();

    const weeklyGoal = await FitnessUnbreakableVow.weeklyGoalsRecords!(weekIndex);
    const mergedRecord = await PhysicalActivityOracle.physicalActivityRecords!(weekIndex);
    const recordsSubmittedForWeek = await getRecordsHistoryForWeek(weekIndex, FitnessUnbreakableVow);
    const penaltyDetails = await getPenaltyAppliedEvent(weeklyGoal, weekIndex, FitnessUnbreakableVow);

    const goals = {
        status: Number(weeklyGoal.status),
        gymVisitsGoalMet: weeklyGoal.wentoToTheGymEnoughTimes,
        run2KmGoalMet: weeklyGoal.ran2km,
        sleptWellGoalMet: weeklyGoal.sleptWell,
        gymVisits: Number(mergedRecord.gymVisits),
        highestDistanceRanInMeters: Number(mergedRecord.runDistanceMeters),
        healthySleepNights: Number(mergedRecord.healthySleepNights),
    };

    return {
        weekIndex: Number(weekIndex),
        goals: goals,
        history: recordsSubmittedForWeek,
        penalty: penaltyDetails,
    };
}

async function getRecordsHistoryForWeek(weekIndex: string, vowContract: EnhancedContract) {
    // We don't have a limit on how many blocks we can query in localhost.
    if (vowContract.network === 'localhoste') {
        return await vowContract.getEvents<PhysicalActivityRecordProcessed>(
            'PhysicalActivityRecordProcessed',
            [weekIndex],
            ['weekIndex', 'runDistanceMeters', 'gymVisits', 'healthySleepNights']
        ) || [];
    }

    // We do have one in prod. So we just fetch from a file that will probably be there...
    const response = await fetch(`/events/${vowContract.contractAddress}/week-${weekIndex}/PhysicalActivityRecordProcessed.json`)

    if (!response.ok) return [];

    try {
        return await response.json() as PhysicalActivityRecordProcessed[];
    } catch(ex) {
        console.error('Unable to fetch events for week: ' + weekIndex, ex);

        return [];
    }
}

async function getPenaltyAppliedEvent(weeklyGoal: any, weekIndex: string, vowContract: EnhancedContract) {
    if (!weeklyGoal.penaltyBlock) return null;

    const eventBlock = Number(weeklyGoal.penaltyBlock);
    const penaltyAppliedEvents = await vowContract.getEvents<PenaltyApplied>('PenaltyApplied', [], ['weekIndex', 'enforcer'], eventBlock - 100, eventBlock + 100);

    return penaltyAppliedEvents.find(item => item.weekIndex == Number(weekIndex)) || null;
}