# FitVow Android App

Android companion app for the FitVow smart‑contract: it collects physical‑activity proofs on device, signs them with a hardware‑protected key, and ships them to the oracle/contract so the agreement can be verified on-chain.

## What it does
- Tracks **gym visits** via Google Play Services geofences.
- Collects **running** and **sleep** sessions from Android Health Connect which are put there by Samsumg Health / Galaxy Watch 4.
- Signs every physical activity record with a hardware protected key (**TEE**) before sending to the Oracle smart contract.
- Shows simple UI for syncing, status, geofence setup and web3 configs.

## Key flows
1) **Geofence setup:** `DefaultMainScreenViewModel.setupGymGeofence()` pulls validator data from the oracle, saves gym configs, and registers geofences with loitering delay/radius from the contract.
2) **Events:** Geofence ENTER/DWELL/EXIT are received by `GymVisitGeofenceEventReceiver`, which starts/validates/finishes a visit and posts a local notification.
3) **Publishing:** `PhysicalActivityOracleService.publishPhysicalActivityEvents()` signs activity blobs and sends them to the oracle contract.

## Building & running
- Requires Android SDK + Google Play Services. AppId/package lives under `app/src/main/java/com/august/fitnessvowsync`.
- Location permissions: both `ACCESS_FINE_LOCATION` and `ACCESS_BACKGROUND_LOCATION` are needed for geofences.
- Geofences depend on Google Play Services and a device/emulator with location services enabled.
