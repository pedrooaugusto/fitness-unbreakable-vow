// SPDX-License-Identifier: MIT 
pragma solidity ^0.8.28;
import { SignatureVerifier } from "./lib/SignatureVerifier.sol";
import { PhysicalActivityRecordListable } from "./lib/PhysicalActivityRecordListable.sol";
import { Ownable } from './lib/Ownable.sol';
import { Expirable } from './lib/Expirable.sol';
import { PhysicalActivityRecord, PhysicalActivityRecordFunctions, Listener, Observable } from './lib/Types.sol';
import { ChainLinkFunctionsParamsProvider, console } from "./lib/Config.sol";

event PhysicalActivityRecordAdded();

// TODO: Rename to WeeklyMetrics
contract PhysicalActivityOracle is SignatureVerifier, PhysicalActivityRecordListable, Observable, Expirable, Ownable {
    using PhysicalActivityRecordFunctions for PhysicalActivityRecord;

    Listener public ORACLE_UPDATE_LISTENER;

    constructor(string memory network, uint256 creationDate, uint256 expirationDate, uint256 secondsInOneWeek)
        Expirable(creationDate, expirationDate, secondsInOneWeek)
        SignatureVerifier(ChainLinkFunctionsParamsProvider.get(network)) {}

    /**
     * @notice Initiates the submission of a new **physical activity record** for the **current week's** data.
     * This function first triggers an **asynchronous signature verification** via an external oracle to ensure the
     * record's **authenticity** and **integrity** before it can be stored.
     * @param signature The cryptographic signature of the `newRecord`. This signature is used by an external oracle
     * to authenticate the sender and verify that the data originates from a trusted, uncompromised source.
     * @param newRecord The `PhysicalActivityRecord` struct containing the activity data to be verified and
     * subsequently included as the current week's record.
     * @dev This function invokes `verifySignature`, which initiates an **asynchronous verification** process
     * involving an **external oracle**. Crucially, this function **does not directly store** the record. Instead,
     * the ultimate storage of the record (or rejection) is managed by the
     * **`signatureVerificationComplete` callback function**, which is triggered once the oracle returns
     * its verification result.
     */
    function pushPhysicalActivityRecord(string calldata signature, PhysicalActivityRecord calldata newRecord) external onlyOwner onlyWhileActive {
        uint8 recordWeekIndex = getWeekIndexOf(uint256(newRecord.timestamp));
        uint8 currentWeekIndex = getCurrentWeekIndex();

        require(recordWeekIndex == currentWeekIndex, "!! Wibbly Wobbly Timey Wimey !!");

        verifySignature(signature, newRecord);
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

        console.log("[PhysicalActivityOracle] Record added.");
    }

    function signatureVerificationComplete(
        bool hasError,
        bool verified,
        PhysicalActivityRecord memory record
    ) internal virtual override {
        if (hasError) {
            revert ("UNKOWN_ERROR");
        } else if (verified == false) {
            revert("CALLER_UNAUTORIZED");
        } else {
            safePushPhysicalActivityRecord(record);
        }
    }
}