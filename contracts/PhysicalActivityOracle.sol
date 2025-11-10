// SPDX-License-Identifier: MIT 
pragma solidity ^0.8.28;

import { PhysicalActivityStats, PublishPhysicalActivityEventRequest, Listener, Observable } from './lib/Types.sol';
import { RunningEventFunctions, RunningEvent, RunningEventValidator } from './lib/Running.sol';
import { SleepEventFunctions, SleepEvent, SleepEventValidator } from './lib/Sleep.sol';
import { GymVisitEventFunctions, GymVisitEvent, GymVisitEventValidator, Location } from './lib/GymVisit.sol';
import { Ownable } from './lib/Ownable.sol';
import { Expirable } from './lib/Expirable.sol';
import { Versioned } from './lib/Versioned.sol';
import { DefaultSignatureVerifier } from './lib/signature/DefaultSignatureVerifier.sol';
import { PhysicalActivityListable } from './lib/PhysicalActivityListable.sol';
import { console } from './lib/variants/console.sol';

event PhysicalActivityStatsUpdate(uint8 indexed weekIndex, PhysicalActivityStats stats);

contract PhysicalActivityOracle is DefaultSignatureVerifier, PhysicalActivityListable, Observable, Expirable, Ownable, Versioned {

    function sleepValidator() public pure returns (SleepEventValidator memory) {
        return SleepEventValidator({
            minimumDurationInMinutes: uint16((6 hours + 10 minutes) / 60), //7h30
            avgBpmLowerBand: 50,
            avgBpmUpperBand: 80
        });
    }

    function runningValidator() public pure returns (RunningEventValidator memory) {
        return RunningEventValidator({
            minimumDistanceInMeters: 1000,
            maximumPaceInSecondsPerKm: uint16(8 minutes), // 7 minutes
            minimumAvgBpm: 110
        });
    }

    function gymVisitValidator() public pure returns (GymVisitEventValidator memory) {
        return GymVisitEventValidator({
            gym1Location: Location({
                latitudeNanoDegree: -228969577, // int(-22.896957745611775 * 1e7)
                longitudeNanoDegree: -432726589 // int(-43.27265899080379 * 1e7)
            }),
            gym2Location: Location({
                latitudeNanoDegree: -228934446, // -22.893444688409225, 
                longitudeNanoDegree: -432925724 // -43.29257247192793
            }),
            minimumVisitTimeInMinutes: uint8((10 minutes) / 60), // 40 minutes
            minimumAvgBpm: 95
        });
    }

    Listener public ORACLE_UPDATE_LISTENER;

    constructor(uint256 creationDate, uint256 expirationDate, uint256 secondsInOneWeek) Expirable(creationDate, expirationDate, secondsInOneWeek) {}

    function publishPhysicalActivityEvent(PublishPhysicalActivityEventRequest calldata request) external onlyOwner onlyWhileActive {
        uint8 currentWeekIndex = getCurrentWeekIndex();

        processRunningEvents(currentWeekIndex, request.running);
        processSleepEvents(currentWeekIndex, request.sleep);
        processGymVisitEvents(currentWeekIndex, request.gymVisit);

        ORACLE_UPDATE_LISTENER.onPhysicalActivityStatsUpdate(currentWeekIndex, physicalActivityStats[currentWeekIndex]);

        emit PhysicalActivityStatsUpdate(currentWeekIndex, physicalActivityStats[currentWeekIndex]);
    }

    function registerPhysicalActivityStatsUpdateListener(address listener) external {
        require(address(ORACLE_UPDATE_LISTENER) == address(0), "Listener already set.");

        ORACLE_UPDATE_LISTENER = Listener(listener);
    }

    function getCurrentWeekPhysicalActivityStats() external view returns (uint8 currentWeekIndex, PhysicalActivityStats memory stats) {
        currentWeekIndex = getCurrentWeekIndex();
        stats = get(currentWeekIndex);

        return (currentWeekIndex, stats);
    }

    function listAllPhysicalActivityStats() external view returns (PhysicalActivityStats[] memory) {
        return list(getCurrentWeekIndex());
    }

    function processRunningEvents(uint8 currentWeekIndex, RunningEvent[] calldata eventos) private {
        for (uint8 i = 0; i < eventos.length; i++) {
            uint8 eventWeekIndex = getWeekIndexOf(uint256(eventos[i].timestamp));
            bytes32 eventHash = RunningEventFunctions.hash(eventos[i]);

            require(verifySignature(eventos[i].signature, eventHash), "youtu.be/LYb_nqU_43w&t=178s");

            if (currentWeekIndex != eventWeekIndex) continue;
            if (RunningEventFunctions.isInvalid(eventos[i], runningValidator())) continue;

            if(insertRunningEvent(eventos[i], eventHash, eventWeekIndex)) {
                RunningEventFunctions.emitProcessedEvent(eventos[i], eventWeekIndex);
            }
        }
    }

    function processSleepEvents(uint8 currentWeekIndex, SleepEvent[] calldata eventos) private {
        for (uint8 i = 0; i < eventos.length; i++) {
            uint8 eventWeekIndex = getWeekIndexOf(uint256(eventos[i].timestamp));
            bytes32 eventHash = SleepEventFunctions.hash(eventos[i]);

            require(verifySignature(eventos[i].signature, eventHash), "youtu.be/LYb_nqU_43w&t=178s");

            if (currentWeekIndex != eventWeekIndex) continue;
            if (SleepEventFunctions.isInvalid(eventos[i], sleepValidator())) continue;

            if(insertSleepEvent(eventos[i], eventHash, eventWeekIndex)) {
                SleepEventFunctions.emitProcessedEvent(eventos[i], eventWeekIndex);
            }
        }
    }

    function processGymVisitEvents(uint8 currentWeekIndex, GymVisitEvent[] calldata eventos) private {
        for (uint8 i = 0; i < eventos.length; i++) {
            uint8 eventWeekIndex = getWeekIndexOf(uint256(eventos[i].timestamp));
            bytes32 eventHash = GymVisitEventFunctions.hash(eventos[i]);

            require(verifySignature(eventos[i].signature, eventHash), "youtu.be/LYb_nqU_43w&t=178s");

            if (currentWeekIndex != eventWeekIndex) continue;
            if (GymVisitEventFunctions.isInvalid(eventos[i], gymVisitValidator())) continue;

            if(insertGymVisitEvent(eventos[i], eventHash, eventWeekIndex)) {
                GymVisitEventFunctions.emitProcessedEvent(eventos[i], eventWeekIndex);
            }
        }
    }
}
