import { WeeklyGoalStructOutput } from "../../typechain-types/contracts/FitnessUnbreakableVow";
import { GymVisitEventStruct, PublishPhysicalActivityEventRequestStruct, RunningEventStruct, SleepEventStruct } from "../../typechain-types/contracts/PhysicalActivityOracle";
import { createSHA256Hash, signGymVisitEvent, signRunningEvent, signSleepEvent } from "./Stats";
import { toUtf8Bytes, sha256 } from 'ethers';

export type PublishEventInteraction = {
    type: 'PUBLISH_ACTIVITY_EVENT',
    data: {
        request: null | PublishPhysicalActivityEventRequestStruct & { useWrongSignature: boolean };
        create: (currentTime: number) => Promise<PublishPhysicalActivityEventRequestStruct & { useWrongSignature: boolean }>
    };
};

export type EnforceAgreementInteraction = {
    type: 'ENFORCE_AGREEMENT',
    data: null;
};

export type TerminateVowInteraction = {
    type: 'TERMINATE_VOW',
    data: null;
};

export type ContractInteraction = PublishEventInteraction | EnforceAgreementInteraction | TerminateVowInteraction;

export type ContractInteractionsInWeek = {
    weekNumber: number,
    interactions: ContractInteraction[];
}

export default function createContractInteractions(numberOfWeeks: number) {
    const weeks = new Array(numberOfWeeks) as ContractInteractionsInWeek[];

    for (let i = 0; i < weeks.length; i++) {
        const interactions = new Array(randomBetween(0, 5)) as ContractInteraction[];

        for (let j = 0; j < interactions.length; j++) {
            interactions[j] = createRandomContractInteraction();
        }

        weeks[i] = { weekNumber: i, interactions: interactions };
    }

    return weeks;
}

function createRandomContractInteraction(): ContractInteraction {
    const interaction = getRandomContractInteraction();

    if (interaction === 1) return createRandomPushActivityRecordInteraction();
    if (interaction === 2) return { type: 'ENFORCE_AGREEMENT', data: null };

    return { type: 'TERMINATE_VOW', data: null };
}

function getRandomContractInteraction() {
    const randomNumber = Math.random();

    if (randomNumber < 0.75) {
        return 1; // 75% chance (range [0, 0.76))
    } else if (randomNumber < 0.95) {
        return 2; // 30% chance (range [0.75, 0.95))
    } else {
        return 3; // 05% chance (range [0.95, 1))
    }
}

function randomBoolean(trueProbability = 0.5) {
    return Math.random() < trueProbability;
}

function randomBetween(min: number, max: number) {
    return Math.floor(Math.random() * (max - min + 1) + min);
}

function createRandomPushActivityRecordInteraction(): PublishEventInteraction {
    return {
        type: 'PUBLISH_ACTIVITY_EVENT',
        data: {
            request: null,
            create: async function(currentTime: number) {
                const running = await createRunningEvents(currentTime);
                const sleep = await createSleepEvents(currentTime);
                const gymVisit = await createGymVisitEvents(currentTime);
                const useWrongSignature = [running, sleep, gymVisit].flat().some(item => item.useWrongSignature);

                const request = { sleep, running, gymVisit, useWrongSignature };

                this.request = request;

                return request;
            }
        }
    }
}

async function createRunningEvents(currentTime: number) {
    const physicalEvents = new Array<RunningEventStruct & { useWrongSignature?: boolean }>(randomBetween(1, 5));

    for (let i = 0; i < physicalEvents.length; i++) {
        const eventP = {
            timestamp: currentTime + i * 10,
            avgBpm: randomBetween(100, 130),
            distanceInMeters: randomBetween(1800, 4000),
            paceInSecondsPerKm: randomBetween(100, 800),
            useWrongSignature: randomBoolean(0.08),
        }

        physicalEvents[i] = await signRunningEvent(eventP);
    }

    return physicalEvents;
}

async function createSleepEvents(currentTime: number) {
    const physicalEvents = new Array<SleepEventStruct & { useWrongSignature?: boolean }>(randomBetween(0, 5));

    for (let i = 0; i < physicalEvents.length; i++) {
        const eventP = {
            timestamp: currentTime + i * 10,
            avgBpm: randomBetween(40, 80),
            durationInMinutes: randomBetween(6 * 60, 10 * 60),
            useWrongSignature: randomBoolean(0.08),
            signature: null as any,
        }

        physicalEvents[i] = await signSleepEvent(eventP);
    }

    return physicalEvents;
}

async function createGymVisitEvents(currentTime: number) {
    const physicalEvents = new Array<GymVisitEventStruct & { useWrongSignature?: boolean }>(randomBetween(1, 5));

    for (let i = 0; i < physicalEvents.length; i++) {
        const eventP = {
            timestamp: currentTime + i * 10,
            avgBpm: randomBetween(100, 160),
            location: { latitudeNanoDegree: -260758108n, longitudeNanoDegree: -280636459n },
            maxBpm: randomBetween(86, 190),
            durationInMinutes: randomBetween(30, 80),
            signature: null as any,
            useWrongSignature: randomBoolean(0.08)
        }

        physicalEvents[i] = await signGymVisitEvent(eventP);
    }

    return physicalEvents;
}

export function numberOfFines(completedGoalsHistory: WeeklyGoalStructOutput[]) {
    const numberOfPenaltiesApplied = completedGoalsHistory.filter(([status, ]) => status !== 1n).length;

    return numberOfPenaltiesApplied;
}
