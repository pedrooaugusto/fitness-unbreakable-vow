// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { P256Signature } from "./signature/Types.sol";

struct SleepEvent {
    P256Signature signature;
    uint32 timestamp;
    uint16 durationInMinutes;
    uint8 avgBpm;
}

struct SleepStats {
    uint8  count;                // # of qualifying nights
    uint16 longestSleepInMinutes;  // max duration in minutes
    uint16 totalSleepInMinutes;    // total qualifying minutes slept
}

event SleepEventProcessed(
    uint8 indexed weekIndex,
    uint32 timestamp,
    uint16 durationInMinutes,
    uint8 avgBpm
);

struct SleepEventValidator {
    uint16 minimumDurationInMinutes;
    uint8 avgBpmLowerBand;
    uint8 avgBpmUpperBand;
}

library SleepEventValidatorFunctions {
    function isValid(SleepEventValidator memory self, SleepEvent calldata evento) internal pure returns (bool) {
        return  evento.durationInMinutes >= self.minimumDurationInMinutes &&
                evento.avgBpm >= self.avgBpmLowerBand &&
                evento.avgBpm <= self.avgBpmUpperBand;
    }
}

library SleepEventFunctions {
    uint32 constant SLEEP = 0x13;

    /**
     * @notice Returns the canonical hash of this sleep session data.
     */ 
    function hash(SleepEvent calldata self) internal pure returns (bytes32) {
        return sha256(
            abi.encodePacked(
                SLEEP,
                uint32(self.timestamp),
                uint32(self.durationInMinutes),
                uint32(self.avgBpm)
            )
        );
    }

    function emitProcessedEvent(SleepEvent calldata self, uint8 weekIndex) internal {
        emit SleepEventProcessed(
            weekIndex,
            self.timestamp,
            self.durationInMinutes,
            self.avgBpm
        );
    }

    /**
     * @notice Merge an accepted/valid sleep event into the rolling weekly SleepStats.
     */
    function mergeWith(SleepStats storage self, SleepEvent calldata evento) internal {
        unchecked { self.count += 1; }
        unchecked { self.totalSleepInMinutes += evento.durationInMinutes; }

        if (evento.durationInMinutes > self.longestSleepInMinutes) {
            self.longestSleepInMinutes = evento.durationInMinutes;
        }
    }

    function isInvalid(SleepEvent calldata self, SleepEventValidator memory validator) internal pure returns (bool) {
        return !SleepEventValidatorFunctions.isValid(validator, self);
    }
}
