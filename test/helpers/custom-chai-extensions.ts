import Chai from 'chai'
import { PhysicalActivityStatsStruct } from '../../typechain-types/contracts/PhysicalActivityOracle';

type ChaiStatic = typeof Chai;
type ChaiUtils = typeof Chai.util;

export function recordEq(chai: ChaiStatic, utils: ChaiUtils) {
    const Assertion = chai.Assertion;

    Assertion.addMethod('equalsStats', function (expectedStruct: PhysicalActivityStatsStruct) {
        const actual = normalizeRecord(this._obj as PhysicalActivityStatsStruct);
        const expected = normalizeRecord(expectedStruct);

        new Assertion(actual).to.include.keys('running', 'gym', 'sleep');
        new Assertion(expected).to.include.keys('running', 'gym', 'sleep');

        const sanitize = (obj: PhysicalActivityStatsStruct) => {
            const { timestamp, ...rest } = obj;
            return rest;
        };

        new Assertion(sanitize(actual)).to.deep.equal(sanitize(expected));
    });

    Assertion.addMethod('emptyRecord', function () {
        const actual = normalizeRecord(this._obj as PhysicalActivityStatsStruct);

        new Assertion(actual).to.include.keys('timestamp', 'healthySleepNights', 'runDistanceMeters', 'gymVisits');

        new Assertion(actual).to.deep.equal({ timestamp: 0n, healthySleepNights: 0n, runDistanceMeters: 0n, gymVisits: 0n });
    });
};

export function normalizeRecord(record: PhysicalActivityStatsStruct): PhysicalActivityStatsStruct {
    return {
        timestamp: record.timestamp,
        running: {
            count: record.running.count,
            totalDistanceInMeters: record.running.totalDistanceInMeters,
            maxAvgBpm: record.running.maxAvgBpm,
            bestPaceInSecondsPerKm: record.running.bestPaceInSecondsPerKm,
            longestDistanceInMeters: record.running.longestDistanceInMeters,
        } as any,
        gym: {
            count: record.gym.count,
            avgBpm: record.gym.avgBpm,
            totalMinutes: record.gym.totalMinutes,
        } as any,
        sleep: {
            count: record.sleep.count,
            longestSleepInMinutes: record.sleep.longestSleepInMinutes,
            totalSleepInMinutes: record.sleep.totalSleepInMinutes,
        } as any,
    } as any;
}

declare global {
    namespace Chai {
        interface Assertion {
            equalsStats(expected: PhysicalActivityStatsStruct): void;
            emptyRecord(): void;
        }
    }
}