import type {
    GetContractOverviewResponse,
    GetWeekDetailsResponse,
    PenaltyApplied,
    GymVisitEventProcessed,
    RunningEventProcessed,
    SleepEventProcessed,
} from '../types';
import loadContract, { type EnhancedContract } from './load-contract';

export async function getWeekDetails(weekIndex: string): Promise<GetWeekDetailsResponse> {
    const { PhysicalActivityOracle, FitnessUnbreakableVow, TimeLordContract } = await loadContract();

    const weeklyGoal = await FitnessUnbreakableVow.weeklyGoalsRecords!(weekIndex);
    const mergedRecord = await PhysicalActivityOracle.physicalActivityStats!(weekIndex) as GetContractOverviewResponse['currentWeekPhysicalActivityStats'];
    const weekHistory = await getPhysicalActivityWeekHistory(weekIndex, PhysicalActivityOracle, TimeLordContract);
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
        history: weekHistory,
        penalty: penaltyDetails,
    };
}

async function getPhysicalActivityWeekHistory(weekIndex: string, oracleContract: EnhancedContract, timeLordContract: EnhancedContract) {
    // We don't have a limit on how many blocks we can query in localhost.
    try {
        const creationDate = Number(await timeLordContract.CREATION_DATE());
        const onlyAfterCreation = (event: { timestamp: number }) => event.timestamp >= creationDate;

        if (oracleContract.network === 'localhost') {
            const [gymVisitEventProcessed, runningEventProcessed, sleepEventProcessed] = await Promise.all([
                oracleContract.getEvents<GymVisitEventProcessed>('GymVisitEventProcessed', [weekIndex], [
                    'weekIndex',
                    'gymLocationLatitudeNanoDegree',
                    'gymLocationLongitudeNanoDegree',
                    'timestamp',
                    'durationInMinutes',
                    'avgBpm',
                    'maxBpm',
                ]),
                oracleContract.getEvents<RunningEventProcessed>('RunningEventProcessed', [weekIndex], [
                    'weekIndex',
                    'timestamp',
                    'distanceInMeters',
                    'paceInSecondsPerKm',
                    'avgBpm',
                ]),
                oracleContract.getEvents<SleepEventProcessed>('SleepEventProcessed', [weekIndex], [
                    'weekIndex',
                    'timestamp',
                    'durationInMinutes',
                    'avgBpm',
                ]),
            ]);

            return {
                gymVisitEventProcessed: gymVisitEventProcessed.filter(onlyAfterCreation),
                runningEventProcessed: runningEventProcessed.filter(onlyAfterCreation),
                sleepEventProcessed: sleepEventProcessed.filter(onlyAfterCreation),
            };
        }

        const fetchJson = async <T>(fileName: string) => {
            const response = await fetch(`/events/${oracleContract.contractAddress}/week-${weekIndex}/${fileName}`);

            if (!response.ok) throw new Error(`Unable to fetch ${fileName}: HTTP ${response.status}`);

            return await response.json() as T;
        };

        const [gymVisitEventProcessed, runningEventProcessed, sleepEventProcessed] = await Promise.all([
            fetchJson<GymVisitEventProcessed[]>('GymVisitEventProcessed.json'),
            fetchJson<RunningEventProcessed[]>('RunningEventProcessed.json'),
            fetchJson<SleepEventProcessed[]>('SleepEventProcessed.json'),
        ]);

        return {
            gymVisitEventProcessed: gymVisitEventProcessed.filter(onlyAfterCreation),
            runningEventProcessed: runningEventProcessed.filter(onlyAfterCreation),
            sleepEventProcessed: sleepEventProcessed.filter(onlyAfterCreation),
        };

    } catch(ex) {
        console.error('Unable to fetch events for week: ' + weekIndex, ex);

        return null;
    }
}

async function getPenaltyAppliedEvent(weeklyGoal: any, weekIndex: string, vowContract: EnhancedContract) {
    if (!weeklyGoal.penaltyBlock) return null;

    const eventBlock = Number(weeklyGoal.penaltyBlock);
    const penaltyAppliedEvents = await vowContract.getEvents<PenaltyApplied>('PenaltyApplied', [], ['weekIndex', 'enforcer'], eventBlock - 10, eventBlock + 10);

    return penaltyAppliedEvents.find(item => item.weekIndex == Number(weekIndex)) || null;
}
