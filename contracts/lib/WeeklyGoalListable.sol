// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { WeeklyGoalStatus, WeeklyGoalFunctions, WeeklyGoal, PhysicalActivityStats } from './Types.sol';
import { TimeLord, ContractPhase } from './timelord/Types.sol';

abstract contract WeeklyGoalListable {
    using WeeklyGoalFunctions for WeeklyGoal;

    uint8 public constant GYM_VISITS_GOAL = 3; // actual value: 3
    uint8 public constant HEALTHY_SLEEP_NIGHTS_GOAL = 3; // actual value: 3
    uint8 public constant RUNNING_SESSIONS_GOAL = 2; // actual value: 2
    uint8 public constant REQUIRED_NUMBER_OF_COMPLETED_GOALS = 2; // actual value: 2

    TimeLord private immutable TIME_LORD;

    uint8 private lastSettledWeek = 255; // [255 + 1 == -1 + 1] :-)
    uint8 public weeklyGoalsRecordsLastEntryKey;
    mapping(uint8 => WeeklyGoal) public weeklyGoalsRecords;

    constructor(TimeLord timeLordAddress) {
        TIME_LORD = timeLordAddress;
    }

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
        uint8 currentWeekIndex = TIME_LORD.getCurrentWeekIndex();

        // 1. Does not run for the current week, only past
        //    weeks*. Starts at the last week that has not
        //    been assigned a terminal status yet.
        uint8 weekIndex;
        unchecked { weekIndex = lastSettledWeek + 1; }

        // 1. If the contract is not active anymore we also include
        //    the current week.
        uint8 includeCurrentWeek = !TIME_LORD.isContractActive() ? 1 : 0;

        for (
            weekIndex;
            weekIndex < currentWeekIndex + includeCurrentWeek;
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
                if (weeklyGoal.wasWasNotCompleted(REQUIRED_NUMBER_OF_COMPLETED_GOALS)) {
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
            // Whenever FAILED_PENDING_PENALTY is set it is imedially overriden
            if (status == WeeklyGoalStatus.FAILED_PENDING_PENALTY) {
                lastSettledWeek = weekIndex;

                return int8(weekIndex);
            }
        }

        return -1;
    }

    function putWeek(uint8 recordWeekIndex, WeeklyGoal memory record) internal {
        if (TIME_LORD.getCurrentWeekIndex() == recordWeekIndex) {
            weeklyGoalsRecords[recordWeekIndex] = record;
            weeklyGoalsRecordsLastEntryKey = recordWeekIndex;
        } else {
            revert("Can't alter past records.");
        }
    }

    function clearWeeks() internal {
        for (uint8 i = 0; i <= weeklyGoalsRecordsLastEntryKey; i++) {
            delete weeklyGoalsRecords[i];
        }

        weeklyGoalsRecordsLastEntryKey = 0;
        lastSettledWeek = 255;
    }

    function listAllWeeks() internal view returns (WeeklyGoal[] memory) {
        uint8 currentWeekIndex = TIME_LORD.getCurrentWeekIndex();
        ContractPhase phase = TIME_LORD.getContractPhase();
        WeeklyGoal[] memory records = new WeeklyGoal[](currentWeekIndex + 1);

        for (uint8 i = 0; i < currentWeekIndex; i++) {
            records[i] = weeklyGoalsRecords[i];

            records[i].status = statusForPastWeek(records[i], phase);
        }

        records[currentWeekIndex] = weeklyGoalsRecords[currentWeekIndex];

        records[currentWeekIndex].status = statusForCurrentWeek(records[currentWeekIndex], phase);

        return records;
    }

    function buildWeeklyGoalFrom(PhysicalActivityStats calldata stats) internal pure returns (WeeklyGoal memory) {
        bool wentoToTheGymEnoughTimes = stats.gym.count >= GYM_VISITS_GOAL;
        bool ran2km = stats.running.count >= RUNNING_SESSIONS_GOAL;
        bool sleptWell = stats.sleep.count >= HEALTHY_SLEEP_NIGHTS_GOAL;

        return WeeklyGoal(
            WeeklyGoalStatus.PENDING_END_OF_WEEK,
            wentoToTheGymEnoughTimes,
            ran2km,
            sleptWell,
            0
        );
    }

    function statusForCurrentWeek(WeeklyGoal memory weeklyGoal, ContractPhase phase) private pure returns (WeeklyGoalStatus) {
        // While active, the current week is always "pending end of week".
        if (phase == ContractPhase.Active) return WeeklyGoalStatus.PENDING_END_OF_WEEK;

        // If already settled (e.g., enforceAgreement() was called), keep it.
        if (weeklyGoal.hasTerminalStatus()) return weeklyGoal.status;

        // If goals were completed but week status not settled, mark as completed.
        if (isCompleted(weeklyGoal)) return WeeklyGoalStatus.COMPLETED;

        // If the contract is already fully expired, nothing else can happen.
        if (phase == ContractPhase.FullyExpired) return WeeklyGoalStatus.NULL;

        // Otherwise we're in grace and it's a failed-but-claimable week.
        return WeeklyGoalStatus.FAILED_PENDING_PENALTY;
    }

    function statusForPastWeek(WeeklyGoal memory weeklyGoal, ContractPhase phase) private pure returns (WeeklyGoalStatus) {
        // Already settled? If yes, just return the status.
        if (weeklyGoal.hasTerminalStatus()) return weeklyGoal.status;

        // If the contract is fully expired:
        if (phase == ContractPhase.FullyExpired) {
            // Mark the week as completed if all the goals were.
            if (weeklyGoal.isPendingEndOfWeek() && isCompleted(weeklyGoal)) return WeeklyGoalStatus.COMPLETED;

            // Keep as null otherwise.
            return WeeklyGoalStatus.NULL;
        }

        // Active or grace: if pending and completed mark as completed;
        if (weeklyGoal.isPendingEndOfWeek() && isCompleted(weeklyGoal)) return WeeklyGoalStatus.COMPLETED;

        // otherwise mark as failed pending penalty.
        return WeeklyGoalStatus.FAILED_PENDING_PENALTY;
    }

    function isCompleted(WeeklyGoal memory weeklyGoal) private pure returns (bool) {
        return weeklyGoal.isCompleted(REQUIRED_NUMBER_OF_COMPLETED_GOALS);
    }
}
