// SPDX-License-Identifier: MIT 
pragma solidity ^0.8.28;
import { SignatureVerifier } from "./lib/SignatureVerifier.sol";
import { Ownable } from './lib/Ownable.sol';
import { Expirable } from './lib/Expirable.sol';
import { Versioned } from "./lib/Versioned.sol";
import { PhysicalActivityRecordListable } from "./lib/PhysicalActivityRecordListable.sol";
import { PhysicalActivityRecord, PhysicalActivityRecordFunctions, P256Signature, Listener, Observable } from './lib/Types.sol';
import { console } from './lib/variants/console.sol';

event PhysicalActivityRecordAdded();

contract PhysicalActivityOracle is SignatureVerifier, PhysicalActivityRecordListable, Observable, Expirable, Ownable, Versioned {
    using PhysicalActivityRecordFunctions for PhysicalActivityRecord;

    /**
     * @notice Registered consumer that receives oracle update callbacks regarding physical activity records.
     * @dev Set exactly once via `registerOnNewPhysicalActivityRecordListener`.
     */
    Listener public ORACLE_UPDATE_LISTENER;

    constructor(uint256 creationDate, uint256 expirationDate, uint256 secondsInOneWeek)
        Expirable(creationDate, expirationDate, secondsInOneWeek)
        SignatureVerifier() {}

    /**
     * @notice Submits a new physical activity record for the current week.
     * @dev Verifies the record's signature on-chain using ECDSA P-256 (secp256r1)
     * with the stored public key. If verification succeeds, the record is stored for the
     * current week; otherwise the call reverts.
     * @param signature The P-256 signature of `newRecord` (low-S, r and s as bytes32).
     * @param newRecord The `PhysicalActivityRecord` to verify and store for the current week.
     */
    function pushPhysicalActivityRecord(P256Signature calldata signature, PhysicalActivityRecord calldata newRecord) external onlyOwner onlyWhileActive {
        uint8 recordWeekIndex = getWeekIndexOf(uint256(newRecord.timestamp));
        uint8 currentWeekIndex = getCurrentWeekIndex();

        require(recordWeekIndex == currentWeekIndex, "!! Wibbly Wobbly Timey Wimey !!");
        require(verifySignature(signature, newRecord), "youtu.be/LYb_nqU_43w&t=178s");

        safePushPhysicalActivityRecord(newRecord);
    }

    /**
     * @notice Retrieves the **physical activity record** for the **current week**.
     * This provides direct access to the latest aggregated activity data on record.
     * @return currentWeekIndex The `uint8` representing the **current week's index** as determined by the contract.
     * @return record The `PhysicalActivityRecord` struct containing all the activity data recorded for the **current week**.
     */
    function getCurrentWeekPhysicalActivityRecord() external view returns (uint8, PhysicalActivityRecord memory) {
        uint8 currentWeekIndex = getCurrentWeekIndex();

        return (currentWeekIndex, get(currentWeekIndex));
    }

    /**
     * @notice Retrieves all physical activity records stored in the contract.
     * @dev Returns an array containing all `PhysicalActivityRecord` structs.
     * @return An array of `PhysicalActivityRecord` representing all recorded physical activities.
     */
    function listAllPhysicalActivityRecords() external view returns (PhysicalActivityRecord[] memory) {
        return list(getCurrentWeekIndex());
    }

    function registerOnNewPhysicalActivityRecordListener(address listener) external {
        require(address(ORACLE_UPDATE_LISTENER) == address(0), "Listener already set.");

        ORACLE_UPDATE_LISTENER = Listener(listener);
    }

    function safePushPhysicalActivityRecord(PhysicalActivityRecord memory newRecord) private {
        uint8 weekIndex = getWeekIndexOf(uint256(newRecord.timestamp));

        PhysicalActivityRecord memory mergedRecord = merge(newRecord, weekIndex);

        ORACLE_UPDATE_LISTENER.onNewPhysicalActivityRecord(weekIndex, mergedRecord);

        emit PhysicalActivityRecordAdded();

        console.log("[PhysicalActivityOracle] New Record Processed.");
    }
}
