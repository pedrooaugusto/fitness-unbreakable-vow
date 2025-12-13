// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { RunningEventValidator } from './Running.sol';
import { SleepEventValidator } from './Sleep.sol';
import { GymVisitEventValidator, Geofence } from './GymVisit.sol';

library PhysicalActivityValidators {
    function sleep() internal pure returns (SleepEventValidator memory) {
        return SleepEventValidator({
            minimumDurationInMinutes: uint16((7 hours) / 60),
            avgBpmLowerBand: 45,
            avgBpmUpperBand: 75
        });
    }

    function running() internal pure returns (RunningEventValidator memory) {
        return RunningEventValidator({
            minimumDistanceInMeters: 2000,
            maximumPaceInSecondsPerKm: uint16(8 minutes + 20 seconds),
            minimumAvgBpm: 110
        });
    }

    function gymVisit() internal pure returns (GymVisitEventValidator memory) {
        return GymVisitEventValidator({
            meierSF:    Geofence({ latitudeNanoDegree: -229034320, longitudeNanoDegree: -432820980, radiusInMeters: 250 }),
            cachambiSF: Geofence({ latitudeNanoDegree: -228871355, longitudeNanoDegree: -432813848, radiusInMeters: 250 }),
            // waka waka https://www.youtube.com/watch?v=pRpeEdMmmQ0&t=55s
            sandtonPL:  Geofence({ latitudeNanoDegree: -260758108, longitudeNanoDegree:  280636459, radiusInMeters: 150 }),
            sandtonVA:  Geofence({ latitudeNanoDegree: -260888460, longitudeNanoDegree:  280602968, radiusInMeters: 150 }),
            minimumVisitTimeInMinutes: uint8((40 minutes) / 60), // 50 minutes
            minimumAvgBpm: 95,
            minimumMaxBpm: 125
        });
    }
}