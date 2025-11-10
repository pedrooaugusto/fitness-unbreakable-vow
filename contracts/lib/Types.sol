// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { SleepEvent, SleepStats } from './Sleep.sol';
import { RunningEvent, RunningStats } from './Running.sol';
import { GymVisitEvent, GymVisitStats } from './GymVisit.sol';

struct PublishPhysicalActivityEventRequest {
    RunningEvent[] running;
    SleepEvent[] sleep;
    GymVisitEvent[] gymVisit;
}

struct PhysicalActivityStats {
    uint32 timestamp;
    RunningStats running;
    GymVisitStats gym;
    SleepStats sleep;
}

enum WeeklyGoalStatus {
    NULL,
    COMPLETED,
    PENDING_END_OF_WEEK,
    FAILED_PENDING_PENALTY,
    FAILED_PENALTY_APPLIED_BY_UPKEEPER,
    FAILED_PENALTY_APPLIED_BY_UNKOWN
}

struct WeeklyGoal {
    WeeklyGoalStatus status;
    bool wentoToTheGymEnoughTimes;
    bool ran2km;
    bool sleptWell;
    uint256 penaltyBlock;
}

library WeeklyGoalFunctions {
    function isCompleted(
        WeeklyGoal memory self,
        uint8 requiredNumberOfGoals
    ) internal pure returns (bool) {
        uint8 ran2km = self.ran2km ? 1 : 0;
        uint8 sleptWell = self.sleptWell ? 1 : 0;
        uint8 wentoToTheGymEnoughTimes = self.wentoToTheGymEnoughTimes ? 1 : 0;

        return (ran2km + sleptWell + wentoToTheGymEnoughTimes) >= requiredNumberOfGoals;
    }

    function hasTerminalStatus(WeeklyGoal memory self) internal pure returns (bool) {
        return self.status == WeeklyGoalStatus.COMPLETED ||
            self.status == WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UNKOWN ||
            self.status == WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UPKEEPER;
    }

    function isPendingEndOfWeek(WeeklyGoal memory self) internal pure returns (bool) {
        return self.status == WeeklyGoalStatus.PENDING_END_OF_WEEK;
    }

    function wasWasNotCompleted(
        WeeklyGoal storage self,
        uint8 requiredNumberOfGoals
    ) internal view returns (bool) {
        uint8 ran2km = self.ran2km ? 1 : 0;
        uint8 sleptWell = self.sleptWell ? 1 : 0;
        uint8 wentoToTheGymEnoughTimes = self.wentoToTheGymEnoughTimes ? 1 : 0;

        return (ran2km + sleptWell + wentoToTheGymEnoughTimes) < requiredNumberOfGoals;
    }
}

interface Observable {
    function registerPhysicalActivityStatsUpdateListener(address listener) external;
}

interface Listener {
    function onPhysicalActivityStatsUpdate(uint8 weekIndex, PhysicalActivityStats calldata record) external;
}
