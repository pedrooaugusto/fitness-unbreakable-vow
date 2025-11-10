import type { GetContractOverviewResponse, GetWeekDetailsResponse, PenaltyApplied, PhysicalActivityStatsUpdate } from '../types';
import loadContract, { type EnhancedContract } from './load-contract';

export async function getWeekDetails(weekIndex: string): Promise<GetWeekDetailsResponse> {
    const { PhysicalActivityOracle, FitnessUnbreakableVow } = await loadContract();

    const weeklyGoal = await FitnessUnbreakableVow.weeklyGoalsRecords!(weekIndex);
    const mergedRecord = await PhysicalActivityOracle.physicalActivityStats!(weekIndex) as GetContractOverviewResponse['currentWeekPhysicalActivityStats'];
    const recordsSubmittedForWeek = await getRecordsHistoryForWeek(weekIndex, PhysicalActivityOracle);
    const penaltyDetails = await getPenaltyAppliedEvent(weeklyGoal, weekIndex, FitnessUnbreakableVow);

    const goals = {
        status: Number(weeklyGoal.status),
        gymVisitsGoalMet: weeklyGoal.wentoToTheGymEnoughTimes,
        run2KmGoalMet: weeklyGoal.ran2km,
        sleptWellGoalMet: weeklyGoal.sleptWell,
        gymVisits: Number(mergedRecord.gym.count),
        runningSessions: Number(mergedRecord.running.count),
        healthySleepNights: Number(mergedRecord.sleep.count),
    };

    return {
        weekIndex: Number(weekIndex),
        goals: goals,
        history: recordsSubmittedForWeek,
        penalty: penaltyDetails,
    };
}

async function getRecordsHistoryForWeek(weekIndex: string, oracleContract: EnhancedContract) {
    // We don't have a limit on how many blocks we can query in localhost.
    if (oracleContract.network === 'localhost') {
        return await oracleContract.getEvents<PhysicalActivityStatsUpdate>('PhysicalActivityStatsUpdate', [weekIndex], ['weekIndex', 'stats']);
    }

    try {
        // We do have one in prod. So we just fetch from a file that will probably be there...
        const response = await fetch(`/events/${oracleContract.contractAddress}/week-${weekIndex}/PhysicalActivityStatsUpdate.json`)

        if (!response.ok) return null;
        return await response.json() as PhysicalActivityStatsUpdate[];
    } catch(ex) {
        console.error('Unable to fetch events for week: ' + weekIndex, ex);

        return null;
    }
}

async function getPenaltyAppliedEvent(weeklyGoal: any, weekIndex: string, vowContract: EnhancedContract) {
    if (!weeklyGoal.penaltyBlock) return null;

    const eventBlock = Number(weeklyGoal.penaltyBlock);
    const penaltyAppliedEvents = await vowContract.getEvents<PenaltyApplied>('PenaltyApplied', [], ['weekIndex', 'enforcer'], eventBlock - 100, eventBlock + 100);

    return penaltyAppliedEvents.find(item => item.weekIndex == Number(weekIndex)) || null;
}