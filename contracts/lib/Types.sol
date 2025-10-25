// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

/**
The future:
*/
struct Running {
    uint16 distanceInMeters;
    uint16 paceInMeters;
    uint8 avgHeartRateInSeconds;
}

struct Sleep {
    uint16 durationInMinutes;
    uint8 avgHeartRateInSeconds;
}

struct GymVisit {
    bytes32 location; // keccak256("$latitude#$longitude") eg: ("-22.596957745611775#-43.27065899080379")
    uint8 durationInMinutes;
}


struct UpdateWeeklyMetricsRequest {
    uint32 timestamp;
    Running running;
    Sleep sleep;
    GymVisit gym;
}

struct PhysicalActivityRecord {
    uint32 timestamp;
    uint16 runDistanceMeters;
    uint8 healthySleepNights;
    uint8 gymVisits;
}

library PhysicalActivityRecordFunctions {
    function mergeWith(
        PhysicalActivityRecord storage self,
        PhysicalActivityRecord memory newRecord
    ) internal {
        self.gymVisits = Math.max8(self.gymVisits, newRecord.gymVisits);
        self.runDistanceMeters = Math.max16(self.runDistanceMeters, newRecord.runDistanceMeters);
        self.healthySleepNights = Math.max8(self.healthySleepNights, newRecord.healthySleepNights);
        self.timestamp = newRecord.timestamp;
    }

    function isNull(
        PhysicalActivityRecord calldata self
    ) internal pure returns (bool) {
        return self.timestamp == 0;
    }

    function isNull(
        PhysicalActivityRecord storage self
    ) internal view returns (bool) {
        return self.timestamp == 0;
    }
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

library Math {
    function min256(uint256 a, uint256 b) internal pure returns (uint256) {
        return a > b ? b : a;
    }

    function max16(uint16 a, uint16 b) internal pure returns (uint16) {
        return a > b ? a : b;
    }

    function max8(uint8 a, uint8 b) internal pure returns (uint8) {
        return a > b ? a : b;
    }
}

interface Observable {
    function registerOnNewPhysicalActivityRecordListener(address listener) external;
}

interface Listener {
    function onNewPhysicalActivityRecord(uint8 weekIndex, PhysicalActivityRecord calldata record) external;
}

interface ISignatureVerifier {
    function isPublicKeySet() external view returns (bool);
}

struct P256Signature {
    bytes32 r;
    bytes32 s;
}

struct P256PublicKey {
    bytes32 x;
    bytes32 y;
}

struct AndroidKeyAttestation {
    string attestationSha256;
    string attestationChallenge;
    string attestationIpfsCID;
}