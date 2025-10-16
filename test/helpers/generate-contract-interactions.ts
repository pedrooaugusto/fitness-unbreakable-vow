import { PhysicalActivityRecordStruct } from "../../typechain-types/contracts/PhysicalActivityOracle";

export type PushActivityRecordInteraction = {
    type: 'PUSH_ACTIVITY_RECORD',
    data: { useWrongSignature?: Boolean } & PhysicalActivityRecordStruct;
};

export type EnforceAgreementInteraction = {
    type: 'ENFORCE_AGREEMENT',
    data: null;
};

export type TerminateVowInteraction = {
    type: 'TERMINATE_VOW',
    data: null;
};

export type ContractInteraction = PushActivityRecordInteraction | EnforceAgreementInteraction | TerminateVowInteraction;
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

    if (randomNumber < 0.6) {
        return 1; // 60% chance (range [0, 0.6))
    } else if (randomNumber < 0.9) {
        return 2; // 30% chance (range [0.6, 0.9))
    } else {
        return 3; // 10% chance (range [0.9, 1))
    }
}

function randomBoolean(trueProbability = 0.5) {
    return Math.random() < trueProbability;
}

function randomBetween(min: number, max: number) {
    return Math.floor(Math.random() * (max - min + 1) + min);
}

function createRandomPushActivityRecordInteraction(): PushActivityRecordInteraction {
    return {
        type: 'PUSH_ACTIVITY_RECORD',
        data: {
            useWrongSignature: randomBoolean(0.25),
            gymVisits: randomBetween(0, 3),
            healthySleepNights: randomBetween(0, 4),
            runDistanceMeters: randomBetween(0, 3000),
            timestamp: 0,
        }
    }
}
