// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import {PhysicalActivityRecord, WeeklyGoalStatus, WeeklyGoalFunctions, WeeklyGoal} from "./Types.sol";
import {Expirable} from "./Expirable.sol";

abstract contract WeeklyGoalListable is Expirable {
    using WeeklyGoalFunctions for WeeklyGoal;

    uint8 private lastSettledWeek = 255; // [255 + 1 == -1 + 1] :-)
    uint8 public weeklyGoalsRecordsLastEntryKey;
    mapping(uint8 => WeeklyGoal) public weeklyGoalsRecords;

    constructor(
        uint256 creationDate,
        uint256 expirationDate
    ) Expirable(creationDate, expirationDate) {}

    /**
     * The function below is written the way it
     * is because it's two main priorities are:
     *   1. Keep gas costs low.
     *      This is why we don't update the
     *      the status of all weeks and then
     *      later just retrive the first
     *      failed one.
     *   2. Keep a record of failed weeks.
     *      Even tough the storage updates
     *      are not exactly necessary to
     *      achieve what this functions does
     *      I think it's worth the trade-off
     *      in gas.
     */
    function findFirstFailedWeek() internal returns (int8) {
        uint8 currentWeekIndex = getCurrentWeekIndex();

        // 1. Does not run for the current week, only past
        //    weeks. Starts at the last week that has not
        //    been assigned a terminal status yet.
        uint8 weekIndex;
        unchecked { weekIndex = lastSettledWeek + 1; }
        for (
            weekIndex;
            weekIndex < currentWeekIndex;
            weekIndex++
        ) {
            WeeklyGoal storage weeklyGoal = weeklyGoalsRecords[weekIndex];
            WeeklyGoalStatus status = weeklyGoal.status;

            // 1. A past week week with an unsasigned status
            //    is considered a failed week.
            if (status == WeeklyGoalStatus.NULL) {
                // This line cost me one BRL cent and is not necessary, but it looks right.
                weeklyGoal.status = WeeklyGoalStatus.FAILED_PENDING_PENALTY;
                lastSettledWeek = weekIndex;
                return int8(weekIndex);
            }

            if (status == WeeklyGoalStatus.PENDING_END_OF_WEEK) {
                // 1. A past week with 'wiat for end of the week'
                //    status where the goals were NOT MET, is
                //    considered a failed week.
                if (weeklyGoal.wasWasNotCompleted()) {
                    // This line cost me one BRL cent and is not necessary, but it looks right.
                    weeklyGoal.status = WeeklyGoalStatus.FAILED_PENDING_PENALTY;
                    lastSettledWeek = weekIndex;
                    return int8(weekIndex);
                }

                // 1. A past week with 'wiat for end of the week'
                //    status where the goals were MET, is
                //    considered a completed week.
                weeklyGoal.status = WeeklyGoalStatus.COMPLETED;
                lastSettledWeek = weekIndex;
            }

            // This should never happen really...
            if (status == WeeklyGoalStatus.FAILED_PENDING_PENALTY) {
                lastSettledWeek = weekIndex;

                return int8(weekIndex);
            }
        }

        return -1;
    }

    function putWeek(uint8 recordWeekIndex, WeeklyGoal memory record) internal {
        if (getCurrentWeekIndex() == recordWeekIndex) {
            weeklyGoalsRecords[recordWeekIndex] = record;
            weeklyGoalsRecordsLastEntryKey = recordWeekIndex;
        } else {
            revert("Can't alter past records.");
        }
    }

    function listAllWeeks() internal view returns (WeeklyGoal[] memory) {
        uint8 currentWeekIndex = getCurrentWeekIndex();

        WeeklyGoal[] memory records = new WeeklyGoal[](currentWeekIndex + 1);
        for (uint8 i = 0; i < currentWeekIndex; i++) {
            records[i] = weeklyGoalsRecords[i];
            WeeklyGoalStatus status = records[i].status;

            if (status == WeeklyGoalStatus.NULL) {
                records[i].status = WeeklyGoalStatus.FAILED_PENDING_PENALTY;
            } else if (status == WeeklyGoalStatus.PENDING_END_OF_WEEK) {
                records[i].status = records[i].isCompleted() ? WeeklyGoalStatus.COMPLETED : WeeklyGoalStatus.FAILED_PENDING_PENALTY;
            }
        }

        records[currentWeekIndex] = weeklyGoalsRecords[currentWeekIndex];
        if (records[currentWeekIndex].status == WeeklyGoalStatus.NULL) {
            records[currentWeekIndex].status = WeeklyGoalStatus.PENDING_END_OF_WEEK;
        }

        // O.o
        if (isContractExpired()) {
            for (uint8 i = 0; i <= currentWeekIndex; i++) {
                if (records[i].status == WeeklyGoalStatus.PENDING_END_OF_WEEK || records[i].status == WeeklyGoalStatus.FAILED_PENDING_PENALTY) {
                    records[i].status = WeeklyGoalStatus.NULL;
                }
            }
        }

        return records;
    }

    function buildWeeklyGoalFrom(
        PhysicalActivityRecord calldata record
    ) internal pure returns (WeeklyGoal memory) {
        // TODO: Rename this, wentToTheGymAtLeastTwice, ranAtLeast2KmInOneGo, sleptWellForAtLeast2Nights;
        bool wentoToTheGymEnoughTimes = record.gymVisits >= 1;
        bool ran2km = record.runDistanceMeters >= 2000;
        bool sleptWell = record.healthySleepNights >= 2;

        return
            WeeklyGoal(
                WeeklyGoalStatus.PENDING_END_OF_WEEK,
                wentoToTheGymEnoughTimes,
                ran2km,
                sleptWell
            );
    }
}
