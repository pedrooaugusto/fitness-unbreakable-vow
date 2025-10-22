// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { PhysicalActivityRecord, PhysicalActivityRecordFunctions } from './Types.sol';

abstract contract PhysicalActivityRecordListable {
    using PhysicalActivityRecordFunctions for PhysicalActivityRecord;

    mapping(uint8 => PhysicalActivityRecord) public physicalActivityRecords;

    function merge(PhysicalActivityRecord memory record, uint8 weekNumber) internal returns (PhysicalActivityRecord memory) {
        PhysicalActivityRecord storage existingRecord = physicalActivityRecords[weekNumber];

        if(existingRecord.isNull()) {
            physicalActivityRecords[weekNumber] = record;
        } else {
            existingRecord.mergeWith(record);
        }

        return existingRecord;
    }

    function get(uint8 weekNumber) internal view returns (PhysicalActivityRecord memory) {
        return (physicalActivityRecords[weekNumber]);
    }

    function list(uint8 currentWeekIndex) internal view returns (PhysicalActivityRecord[] memory) {
        PhysicalActivityRecord[] memory records = new PhysicalActivityRecord[](currentWeekIndex + 1);

        for (uint8 i = 0; i <= currentWeekIndex; i++) {
            records[i] = physicalActivityRecords[i];
        }

        return records;
    }
}