// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { SignatureVerifier } from './signature/Types.sol';
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

library Environment {
    function isLocalhost() internal view returns (bool) {
        return block.chainid == 31337;
    }
}

interface Observable {
    function registerPhysicalActivityStatsUpdateListener(address listener) external;
}

interface Listener {
    function onPhysicalActivityStatsUpdate(uint8 weekIndex, PhysicalActivityStats calldata record) external;
}

interface TimeBound {
    function TIME_LORD() external view returns(TimeLord);
}

interface TimeLord {
    /// @notice Unix timestamp (seconds) when the active phase ends
    /// and the grace period begins.
    /// @return Unix timestamp in seconds.
    function EXPIRATION_DATE() external view returns(uint256);
    /// @notice Unix timestamp (seconds) when the contract was created
    /// and the schedule starts.
    /// @return Unix timestamp in seconds.
    function CREATION_DATE() external view returns(uint256);
    /// @notice Number of seconds that define one week for the schedule.
    /// @return Number of seconds in one week.
    function SECONDS_IN_ONE_WEEK() external view returns(uint256);
    /// @notice Length of the grace period (in seconds) after
    /// `EXPIRATION_DATE` before the contract is fully expired.
    function GRACE_PERIOD() external view returns(uint256);

    function isContractActive() external view returns (bool);
    function isContractInGracePeriod() external view returns (bool);
    function isContractFullyExpired() external view returns (bool);
    function getContractPhase() external view returns (ContractPhase);
    function getCurrentWeekIndex() external view returns (uint8);
    function getWeekIndexOf(uint256 timestamp) external view returns (uint8);
}

enum ContractPhase { Active, Grace, FullyExpired }

interface OracleInterface is TimeBound, Observable, SignatureVerifier {}