// SPDX-License-Identifier: MIT 
pragma solidity ^0.8.28;

import { PhysicalActivityStats, PublishPhysicalActivityEventRequest, FitVowInterface, Observable } from './lib/Types.sol';
import { TimeLord, TimeBound } from './lib/timelord/Types.sol';
import { RunningEventFunctions, RunningEvent, RunningEventValidator } from './lib/Running.sol';
import { SleepEventFunctions, SleepEvent, SleepEventValidator } from './lib/Sleep.sol';
import { GymVisitEventFunctions, GymVisitEvent, GymVisitEventValidator } from './lib/GymVisit.sol';
import { PhysicalActivityValidators } from './lib/PhysicalActivityValidator.sol';
import { Ownable } from './lib/Ownable.sol';
import { Versioned } from './lib/Versioned.sol';
import { DefaultSignatureVerifier } from './lib/signature/DefaultSignatureVerifier.sol';
import { P256PublicKey, AndroidKeyAttestation } from './lib/signature/Types.sol';
import { PhysicalActivityListable } from './lib/PhysicalActivityListable.sol';
import { console } from './lib/variants/console.sol';

event PhysicalActivityStatsUpdate(uint8 indexed weekIndex, PhysicalActivityStats stats);
event EmergencyPublicKeyChange(uint256 price, string reason, string oldKey);

contract PhysicalActivityOracle is DefaultSignatureVerifier, PhysicalActivityListable, Observable, TimeBound, Ownable, Versioned {
    uint8 public constant ONE_TIME_EMERGENCY_KEY_CHANGE_PRICE = 35; // 35% of the vow contract balance
    bool  public   ONE_TIME_EMERGENCY_KEY_CHANGE_USED;

    TimeLord public immutable TIME_LORD;
    FitVowInterface public FITNESS_UNBREAKABLE_VOW;

    constructor(TimeLord timeLordAddress) {
        TIME_LORD = timeLordAddress;
    }

    /**
     * @notice Submits batches of running, sleep, and gym visit events for the current week.
     * @dev Verifies P-256 signatures, validates each event, updates aggregates, and emits processed events plus a weekly stats update.
     * Only callable by the vow (contract owner) while the oracle is active.
     */
    function publishPhysicalActivityEvent(PublishPhysicalActivityEventRequest calldata request) external onlyOwner onlyWhileActive {
        uint8 currentWeekIndex = TIME_LORD.getCurrentWeekIndex();

        processRunningEvents(currentWeekIndex, request.running);
        processSleepEvents(currentWeekIndex, request.sleep);
        processGymVisitEvents(currentWeekIndex, request.gymVisit);

        FITNESS_UNBREAKABLE_VOW.onPhysicalActivityStatsUpdate(currentWeekIndex, physicalActivityStats[currentWeekIndex]);

        emit PhysicalActivityStatsUpdate(currentWeekIndex, physicalActivityStats[currentWeekIndex]);
    }

    /**
     * @notice Registers a single stats update listener that will receive callbacks after each publish.
     * @dev Intended to be set to the FitnessUnbreakableVow contract; can only be set once and must be called by the oracle owner.
     */
    function registerPhysicalActivityStatsUpdateListener(address listener) external onlyOwnerOrigin {
        require(address(FITNESS_UNBREAKABLE_VOW) == address(0), "Listener already set.");

        FITNESS_UNBREAKABLE_VOW = FitVowInterface(listener);
    }

    /**
     * @notice Sets the public key and attestation metadata used to verify submitted events. Once set it cannot be altered.
     * @dev Forwards to the internal setter defined in DefaultSignatureVerifier; callable only by the owner.
     */
    function setPublicKey(P256PublicKey calldata publicKey, AndroidKeyAttestation calldata keyAttestation) external onlyOwner {
        _setPublicKey(publicKey, keyAttestation);
    }

    /**
     * @notice Allows the owner to swap the P-256 public key using a one-time, high-cost emergency escape hatch.
     * @dev Charges 35% of the FitnessUnbreakableVow contract balance and forwards it to the Giveth charity wallet.
     * Can only be executed once; emits the previous attestation CID for auditability. The reality of living in 
     * a soon to be narcostate makes this method necessary in case my phone is stolen :(
     * @param newPublicKey Replacement public key (x, y) coordinates.
     * @param newKeyAttestation Android Key Attestation metadata tied to the replacement key.
     * @param changeReason Free-form description explaining why the emergency change was needed.
     */
    function expensiveOneTimeEmergencyPublicKeyChange(
        P256PublicKey calldata newPublicKey,
        AndroidKeyAttestation calldata newKeyAttestation,
        string calldata changeReason
    ) external payable onlyOwner {
        uint256 keyChangePrice = (ONE_TIME_EMERGENCY_KEY_CHANGE_PRICE * address(FITNESS_UNBREAKABLE_VOW).balance) / 100;
        string memory previousAttestationCid = PUBLIC_KEY_ATTESTATION.attestationIpfsCID;

        require(!ONE_TIME_EMERGENCY_KEY_CHANGE_USED, "Public key already changed");
        require(msg.value >= keyChangePrice, "Payment Required");

        // emergency, emergency, paging dr Beat https://www.youtube.com/shorts/8eeejFE2sIQ
        _emergencyPublicKeyChange(newPublicKey, newKeyAttestation);
        _sendEth(FITNESS_UNBREAKABLE_VOW.GIVETH_WALLET_ADDRESS(), msg.value);

        emit EmergencyPublicKeyChange(keyChangePrice, changeReason, previousAttestationCid);

        ONE_TIME_EMERGENCY_KEY_CHANGE_USED = true;
    }

    function getCurrentWeekPhysicalActivityStats() external view returns (uint8 currentWeekIndex, PhysicalActivityStats memory stats) {
        currentWeekIndex = TIME_LORD.getCurrentWeekIndex();
        stats = get(currentWeekIndex);

        return (currentWeekIndex, stats);
    }

    function listAllPhysicalActivityStats() external view returns (PhysicalActivityStats[] memory) {
        return list(TIME_LORD.getCurrentWeekIndex());
    }

    function processRunningEvents(uint8 currentWeekIndex, RunningEvent[] calldata eventos) private {
        for (uint8 i = 0; i < eventos.length; i++) {
            uint8 eventWeekIndex = TIME_LORD.getWeekIndexOf(uint256(eventos[i].timestamp));
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
            uint8 eventWeekIndex = TIME_LORD.getWeekIndexOf(uint256(eventos[i].timestamp));
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
            uint8 eventWeekIndex = TIME_LORD.getWeekIndexOf(uint256(eventos[i].timestamp));
            bytes32 eventHash = GymVisitEventFunctions.hash(eventos[i]);

            require(verifySignature(eventos[i].signature, eventHash), "youtu.be/LYb_nqU_43w&t=178s");

            if (currentWeekIndex != eventWeekIndex) continue;
            if (GymVisitEventFunctions.isInvalid(eventos[i], gymVisitValidator())) continue;

            if(insertGymVisitEvent(eventos[i], eventHash, eventWeekIndex)) {
                GymVisitEventFunctions.emitProcessedEvent(eventos[i], eventWeekIndex);
            }
        }
    }

    function sleepValidator() public pure returns (SleepEventValidator memory) {
        return PhysicalActivityValidators.sleep();
    }

    function runningValidator() public pure returns (RunningEventValidator memory) {
        return PhysicalActivityValidators.running();
    }

    function gymVisitValidator() public pure returns (GymVisitEventValidator memory) {
        return PhysicalActivityValidators.gymVisit();
    }

    modifier onlyWhileActive() {
        require(TIME_LORD.isContractActive(), "Contract has expired.");
        _;
    }

    function _sendEth(address to, uint256 amount) private {
        (bool success, ) = to.call{value: amount}("");

        require(success, "ETH transfer failed");
    }
}
