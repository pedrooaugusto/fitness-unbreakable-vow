// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { PhysicalActivityRecord, WeeklyGoalStatus, WeeklyGoalCompletionResultFunctions, WeeklyGoalsCompletionResult, PhysicalActivityRecordFunctions } from "./Types.sol";
import { Expirable } from './Expirable.sol';

abstract contract WeeklyGoalsRecords is Expirable {
    using WeeklyGoalCompletionResultFunctions for WeeklyGoalsCompletionResult;

    uint8 private lastSettledWeek = 255;
    uint8 public weeklyGoalsRecordsLastEntryKey;
    mapping(uint8 => WeeklyGoalsCompletionResult) public weeklyGoalsRecords;

    constructor(
        uint256 creationDate,
        uint256 expirationDate
    ) Expirable(creationDate, expirationDate) {}

    function updateWeeklyStatusesAndFindFirstFailure() internal returns(int8) {
        uint8 currentWeekIndex = getCurrentWeekIndex();

        for(uint8 weekIndex = 0; weekIndex < currentWeekIndex; weekIndex++) {
            WeeklyGoalsCompletionResult storage record = weeklyGoalsRecords[weekIndex];

            if (record.isNull()) {
                weeklyGoalsRecords[weekIndex] = WeeklyGoalCompletionResultFunctions.buildFailedGoal();

                return int8(weekIndex);
            }

            if (record.isPendingEndOfWeek()) {
                if (!record.isCompleted()) {
                    record.status = WeeklyGoalStatus.FAILED_PENDING_PENALTY;

                    return int8(weekIndex);
                }

                record.status = WeeklyGoalStatus.COMPLETED;
            }
        }

        return -1;
    }

    function updateAllWeekStatuses() internal {
        uint8 currentWeekIndex = getCurrentWeekIndex();

        for(uint8 weekIndex = 0; weekIndex < currentWeekIndex; weekIndex++) {
            WeeklyGoalsCompletionResult storage record = weeklyGoalsRecords[weekIndex];

            if (record.isNull()) {
                record.status = WeeklyGoalStatus.FAILED_PENDING_PENALTY;
            } else if (record.isPendingEndOfWeek()) {
                record.status = record.isCompleted() ? WeeklyGoalStatus.COMPLETED : WeeklyGoalStatus.FAILED_PENDING_PENALTY;
            }
        }
    }

    function findFirstWeekPendingPenalty() internal view returns (int8) {
        uint8 currentWeekIndex = getCurrentWeekIndex();

        for(uint8 weekIndex = 0; weekIndex <= currentWeekIndex; weekIndex++) {
            if (weeklyGoalsRecords[weekIndex].status == WeeklyGoalStatus.FAILED_PENDING_PENALTY) {
                return int8(weekIndex);
            }
        }

        return -1;
    }

    function findFirstFailedWeek() internal returns (int8) {
        uint8 currentWeekIndex = getCurrentWeekIndex();

        for(uint8 weekIndex = lastSettledWeek + 1; weekIndex < currentWeekIndex; weekIndex++) {
            WeeklyGoalsCompletionResult storage record = weeklyGoalsRecords[weekIndex];

            if (record.isNull()) {
                record.status = WeeklyGoalStatus.FAILED_PENALTY_APPLIED;
                lastSettledWeek = weekIndex;
                return int8(weekIndex);
            }

            if (record.isPendingEndOfWeek()) {
                if (record.goalsWereNotAchvied()) {
                    record.status = WeeklyGoalStatus.FAILED_PENALTY_APPLIED;
                    lastSettledWeek = weekIndex;
                    return int8(weekIndex);
                }

                record.status = WeeklyGoalStatus.COMPLETED;
            }

            lastSettledWeek = weekIndex;
        }

        return -1;
    }

    function putWeek(uint8 recordWeekIndex, WeeklyGoalsCompletionResult memory record) internal {
        if (weeklyGoalsRecords[recordWeekIndex].isNull() || getCurrentWeekIndex() == recordWeekIndex) {
            weeklyGoalsRecords[recordWeekIndex] = record;
            weeklyGoalsRecordsLastEntryKey = recordWeekIndex;
        } else {
            revert("Can't alter past records.");
        }
    }

    function listAllWeeks() internal view returns (WeeklyGoalsCompletionResult[] memory) {
        uint8 currentWeekIndex = getCurrentWeekIndex();

        WeeklyGoalsCompletionResult[] memory records = new WeeklyGoalsCompletionResult[](currentWeekIndex + 1);
        // Even if weeklyGoalsRecords was not updated
        // to refolect a week is over
        // when reading we have ensure it
        for (uint8 i = 0; i <= currentWeekIndex; i++) {
            records[i] = weeklyGoalsRecords[i];
            // Adjust cases where the week is over
            // if (records[i].isNull()) {}
        }
        return records;
    }

    function buildWeeklyGoalsFrom(PhysicalActivityRecord memory record) internal pure returns (WeeklyGoalsCompletionResult memory) {
        // TODO: Rename this, wentToTheGymAtLeastTwice, ranAtLeast2KmInOneGo, sleptWellForAtLeast2Nights; 
        bool wentoToTheGymEnoughTimes = record.gymVisits >= 1;
        bool ran2km = record.runDistanceMeters >= 2000;
        bool sleptWell = record.healthySleepNights >= 2;

        return WeeklyGoalsCompletionResult(WeeklyGoalStatus.PENDING_END_OF_WEEK, wentoToTheGymEnoughTimes, ran2km, sleptWell);
    }
}