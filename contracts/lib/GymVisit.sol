// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { P256Signature } from "./signature/Types.sol";

struct Location {
    // latitude and longitude are stored as degrees * 1e7 (nano-degree precision)
    int64 latitudeNanoDegree;
    int64 longitudeNanoDegree;
}

struct Geofence {
    // latitude and longitude are stored as degrees * 1e7 (nano-degree precision)
    int64 latitudeNanoDegree;
    int64 longitudeNanoDegree;
    uint16 radiusInMeters;
}

struct GymVisitEvent {
    P256Signature signature;
    Location location; // { latitudeNanoDegree: int(-22.5454554 * 1e7), longitudeNanoDegree: int(-40.3232 * 1e7) }
    uint32 timestamp;
    uint8 durationInMinutes;
    uint8 avgBpm;
    uint8 maxBpm;
}

struct GymVisitStats {
    uint8 count;          // # of qualifying gym visits
    uint16 totalMinutes;   // sum of durations (minutes)
    uint8 avgBpm;         // running average of avgBpm across visits
}

event GymVisitEventProcessed(
    uint8 indexed weekIndex,
    int64 gymLocationLatitudeNanoDegree,
    int64 gymLocationLongitudeNanoDegree,
    uint32 timestamp,
    uint8 durationInMinutes,
    uint8 avgBpm,
    uint8 maxBpm
);

struct GymVisitEventValidator {
    Geofence gymLoc1;
    Geofence gymLoc2;
    Geofence gymLoc3;
    Geofence gymLoc4;
    uint8 minimumVisitTimeInMinutes;
    uint8 minimumAvgBpm;
    uint8 minimumMaxBpm;
}

library GymVisitEventValidatorFunctions {
    function isValid(
        GymVisitEventValidator memory self,
        GymVisitEvent calldata evento
    ) internal pure returns (bool) {
        Geofence[4] memory gymLocations;

        gymLocations[0] = self.gymLoc1;
        gymLocations[1] = self.gymLoc2;
        gymLocations[2] = self.gymLoc3;
        gymLocations[3] = self.gymLoc4;

        return  isValidGymLocation(evento.location, gymLocations) &&
                evento.durationInMinutes >= self.minimumVisitTimeInMinutes &&
                evento.avgBpm >= self.minimumAvgBpm &&
                evento.maxBpm >= self.minimumMaxBpm;
    }

    function isValidGymLocation(Location calldata location, Geofence[4] memory geofences) private pure returns (bool) {
        for(uint i = 0; i < geofences.length; i++) {
            if (isInsideGeofence(location, geofences[i])) {
                return true;
            }
        }

        return false;
    }

    function isInsideGeofence(Location calldata a, Geofence memory b) private pure returns (bool) {
        return a.latitudeNanoDegree == b.latitudeNanoDegree && a.longitudeNanoDegree == b.longitudeNanoDegree;
    }
}

library GymVisitEventFunctions {
    uint32 constant GYM = 0x17;

    function hash(GymVisitEvent calldata self) internal pure returns (bytes32) {
        return sha256(abi.encodePacked(
            GYM,
            self.location.latitudeNanoDegree,
            self.location.longitudeNanoDegree,
            uint32(self.timestamp),
            uint32(self.durationInMinutes),
            uint32(self.avgBpm),
            uint32(self.maxBpm)
        ));
    }

    function emitProcessedEvent(GymVisitEvent calldata self, uint8 weekIndex) internal {
        emit GymVisitEventProcessed(
            weekIndex,
            self.location.latitudeNanoDegree,
            self.location.longitudeNanoDegree,
            self.timestamp,
            self.durationInMinutes,
            self.avgBpm,
            self.maxBpm
        );
    }

    function mergeWith(GymVisitStats storage self, GymVisitEvent calldata evento) internal {
        uint8 oldCount = self.count;

        unchecked { self.count = oldCount + 1; }

        unchecked { self.totalMinutes += evento.durationInMinutes; }

        // update avgBpm as running unweighted mean of visit avgBpm
        if (oldCount == 0) {
            // first visit this week
            self.avgBpm = evento.avgBpm;
        } else {
            uint32 weightedSum = uint32(self.avgBpm) * uint32(oldCount) + uint32(evento.avgBpm);

            uint8 newAvg = uint8(weightedSum / uint32(oldCount + 1));

            self.avgBpm = newAvg;
        }
    }

    function isInvalid(GymVisitEvent calldata self, GymVisitEventValidator memory validator) internal pure returns (bool) {
        return !GymVisitEventValidatorFunctions.isValid(validator, self);
    }
}
