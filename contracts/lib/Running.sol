// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { P256Signature } from './signature/Types.sol';

struct RunningEvent {
    P256Signature signature;
    uint32 timestamp;
    uint16 distanceInMeters;
    uint16 paceInSecondsPerKm;
    uint8 avgBpm;
}

struct RunningStats {
    uint8  count;                    // # of qualifying runs
    uint16 longestDistanceInMeters;    // max distance (m)
    uint16 totalDistanceInMeters;      // sum of distances (m)
    uint16 bestPaceInSecondsPerKm;   // lower = faster; 0 means unset
    uint8  maxAvgBpm;                // highest avg heart rate in a valid run
}

event RunningEventProcessed(
    uint8 indexed weekIndex,
    uint32 timestamp,
    uint16 distanceInMeters,
    uint16 paceInSecondsPerKm,
    uint8 avgBpm
);

struct RunningEventValidator {
    uint16 minimumDistanceInMeters;
    uint16 maximumPaceInSecondsPerKm;
    uint8 minimumAvgBpm;
}

library RunningEventValidatorFunctions {
    function isValid(RunningEventValidator memory self, RunningEvent calldata evento) internal pure returns (bool) {
        return  evento.distanceInMeters >= self.minimumDistanceInMeters &&
                evento.paceInSecondsPerKm <= self.maximumPaceInSecondsPerKm &&
                evento.avgBpm >= self.minimumAvgBpm;
    }
}

library RunningEventFunctions {
    uint32 constant RUNNING = 0x11;

    function hash(RunningEvent calldata self) internal pure returns (bytes32) {
        return sha256(
            abi.encodePacked(
                RUNNING,
                uint32(self.timestamp),
                uint32(self.distanceInMeters),
                uint32(self.paceInSecondsPerKm),
                uint32(self.avgBpm)
            )
        );
    }

    function emitProcessedEvent(RunningEvent calldata self, uint8 weekIndex) internal {
        emit RunningEventProcessed(
            weekIndex,
            self.timestamp,
            self.distanceInMeters,
            self.paceInSecondsPerKm,
            self.avgBpm
        );
    }

    function mergeWith(RunningStats storage self, RunningEvent calldata evento) internal {
        unchecked { self.count += 1; }
        unchecked { self.totalDistanceInMeters += evento.distanceInMeters; }

        if (evento.distanceInMeters > self.longestDistanceInMeters) {
            self.longestDistanceInMeters = evento.distanceInMeters;
        }

        if (self.bestPaceInSecondsPerKm == 0 || evento.paceInSecondsPerKm < self.bestPaceInSecondsPerKm) {
            self.bestPaceInSecondsPerKm = evento.paceInSecondsPerKm;
        }

        if (evento.avgBpm > self.maxAvgBpm) {
            self.maxAvgBpm = evento.avgBpm;
        }
    }

    function isInvalid(RunningEvent calldata self, RunningEventValidator memory validator) internal pure returns (bool) {
        return !RunningEventValidatorFunctions.isValid(validator, self);
    }
}

function toMinutes(uint256 duration) pure returns (uint16) { return uint16(duration / 60); }