// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { PhysicalActivityStats } from './Types.sol';
import { SleepEvent, SleepStats, SleepEventFunctions } from './Sleep.sol';
import { RunningEvent, RunningStats, RunningEventFunctions } from './Running.sol';
import { GymVisitEvent, GymVisitStats, GymVisitEventFunctions } from './GymVisit.sol';

abstract contract PhysicalActivityListable {
    using SleepEventFunctions for SleepStats;
    using RunningEventFunctions for RunningStats;
    using GymVisitEventFunctions for GymVisitStats;

    mapping(uint8 => PhysicalActivityStats) public physicalActivityStats;
    mapping(bytes32 => bool) public processedEvents;


    function insertRunningEvent(RunningEvent calldata evento, bytes32 eventId, uint8 weekNumber) internal returns (bool) {
        if (processedEvents[eventId]) return false;

        processedEvents[eventId] = true;

        physicalActivityStats[weekNumber].running.mergeWith(evento);
        physicalActivityStats[weekNumber].timestamp = evento.timestamp;

        return true;
    }

    function insertSleepEvent(SleepEvent calldata evento, bytes32 eventId, uint8 weekNumber) internal returns (bool) {
        if (processedEvents[eventId]) return false;

        processedEvents[eventId] = true;

        physicalActivityStats[weekNumber].sleep.mergeWith(evento);
        physicalActivityStats[weekNumber].timestamp = evento.timestamp;

        return true;
    }

    function insertGymVisitEvent(GymVisitEvent calldata evento, bytes32 eventId, uint8 weekNumber) internal returns (bool) {
        if (processedEvents[eventId]) return false;

        processedEvents[eventId] = true;

        physicalActivityStats[weekNumber].gym.mergeWith(evento);
        physicalActivityStats[weekNumber].timestamp = evento.timestamp;

        return true;
    }

    function get(uint8 weekNumber) internal view returns (PhysicalActivityStats memory) {
        return (physicalActivityStats[weekNumber]);
    }

    function list(uint8 currentWeekIndex) internal view returns (PhysicalActivityStats[] memory) {
        PhysicalActivityStats[] memory records = new PhysicalActivityStats[](currentWeekIndex + 1);

        for (uint8 i = 0; i <= currentWeekIndex; i++) {
            records[i] = physicalActivityStats[i];
        }

        return records;
    }
}