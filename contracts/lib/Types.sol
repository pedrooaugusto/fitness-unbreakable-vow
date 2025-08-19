pragma solidity ^0.8.28;

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
    FAILED_PENALTY_APPLIED
}

struct WeeklyGoal {
    WeeklyGoalStatus status;
    bool wentoToTheGymEnoughTimes;
    bool ran2km;
    bool sleptWell;
}


library WeeklyGoalFunctions {
    function isCompleted(
        WeeklyGoal storage self
    ) internal view returns (bool) {
        bool ran2km = self.ran2km;
        bool sleptWell = self.sleptWell;
        bool wentoToTheGymEnoughTimes = self.wentoToTheGymEnoughTimes;

        if ((wentoToTheGymEnoughTimes && ran2km) || (wentoToTheGymEnoughTimes && sleptWell) || (ran2km && sleptWell)) {
            return true;
        } else {
            return false;
        }
    }

    function wasWasNotCompleted(
        WeeklyGoal storage self
    ) internal view returns (bool) {
        return !isCompleted(self);
    }
}

library Math {
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
    function onNewPhysicalActivityRecord(uint8 weekIndex, PhysicalActivityRecord memory record) external;
}