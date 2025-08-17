import Chai from 'chai'
import { PhysicalActivityRecordStruct } from '../../typechain-types/contracts/PhysicalActivityOracle';

type ChaiStatic = typeof Chai;
type ChaiUtils = typeof Chai.util;

export function recordEq(chai: ChaiStatic, utils: ChaiUtils) {
    const Assertion = chai.Assertion;

    Assertion.addMethod('equalsRecord', function (expectedStruct: PhysicalActivityRecordStruct) {
        const actual = normalizeRecord(this._obj as PhysicalActivityRecordStruct);
        const expected = normalizeRecord(expectedStruct);

        new Assertion(actual).to.include.keys('healthySleepNights', 'runDistanceMeters', 'gymVisits');
        new Assertion(expected).to.include.keys('healthySleepNights', 'runDistanceMeters', 'gymVisits');

        const sanitize = (obj: PhysicalActivityRecordStruct) => {
            const { timestamp, ...rest } = obj;
            return rest;
        };

        new Assertion(sanitize(actual)).to.deep.equal(sanitize(expected));
    });

    Assertion.addMethod('emptyRecord', function () {
        const actual = normalizeRecord(this._obj as PhysicalActivityRecordStruct);

        new Assertion(actual).to.include.keys('timestamp', 'healthySleepNights', 'runDistanceMeters', 'gymVisits');

        new Assertion(actual).to.deep.equal({ timestamp: 0n, healthySleepNights: 0n, runDistanceMeters: 0n, gymVisits: 0n });
    });
};

export function normalizeRecord(record: PhysicalActivityRecordStruct): PhysicalActivityRecordStruct {
    return {
        timestamp: record.timestamp,
        healthySleepNights: record.healthySleepNights,
        runDistanceMeters: record.runDistanceMeters,
        gymVisits: record.gymVisits,
    };
}

declare global {
    namespace Chai {
        interface Assertion {
            equalsRecord(expected: PhysicalActivityRecordStruct): void;
            emptyRecord(): void;
        }
    }
}