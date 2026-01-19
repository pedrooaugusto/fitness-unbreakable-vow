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

## Sign-and-forget APK signing (amnesiac releases)

FitVow uses an intentionally **non-upgradable APK signing model** to protect the integrity of the experiment.

The `app/amnesiac-apk-signer.sh` script signs the APK with a **one-time, ephemeral signing key** and then *permanently deletes* the keystore. After this step, it is cryptographically impossible to produce another APK signed with the same key.

This is not a mistake — it is the security guarantee.

### Why this exists

On Android, the app’s signing key defines its identity. If a future APK is signed with a *different* key, Android treats it as a **different app**, which forces:
- a full uninstall,
- loss of app data,
- and crucially, **destruction of all hardware-backed (TEE) keys** created by the original app.

FitVow relies on this property.

The first installed APK:
- generates a hardware-backed key inside the device’s TEE,
- registers the corresponding public key with the on-chain oracle,
- and becomes the *only* app instance capable of submitting valid activity proofs.

By making the signing key unrecoverable, I remove my own ability to:
- ship a modified build,
- keep the same app identity,
- and reuse the original TEE keys to fake or alter activity data.

Any attempt to do so would require reinstalling a differently signed APK, which the oracle would immediately reject as coming from an unknown app/device.

### What this protects against

This model deliberately raises the cost of cheating by the app author:
- No silent upgrades
- No hotfixes with the same identity
- No “just one more build” to bypass validation logic

If I want to change behavior, I must pay the full price:
- uninstall the app,
- lose the TEE keys,
- and invoke the explicit on-chain recovery / key-rotation mechanism (which carries a financial penalty).

In other words: **I cannot fix the app without also weakening my own position in the vow.**

## Building & running
- Requires Android SDK + Google Play Services. AppId/package lives under `app/src/main/java/com/august/fitnessvowsync`.
- Location permissions: both `ACCESS_FINE_LOCATION` and `ACCESS_BACKGROUND_LOCATION` are needed for geofences.
- Geofences depend on Google Play Services and a device/emulator with location services enabled.
