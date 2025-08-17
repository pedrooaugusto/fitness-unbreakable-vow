// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { WeeklyGoalStatus, WeeklyGoalsCompletionResult, PhysicalActivityRecord, Oracle, PhysicalActivityOracleListener } from './lib/Types.sol';
import { Ownable } from './lib/Ownable.sol';
import { WeeklyGoalsRecords } from './lib/WeeklyGoalsRecords.sol';

event NoPenaltyApplied();
event PenaltyApplied(uint256 penaltyAmount, address receiver);
event VowExpired(uint256 releasedFunds, address receiver);

/**
 * @title FitnessUnbreakableVow: Penalizes Physical Inactivity with Fund Deduction.
 * @notice This contract enforces physical activity goals by deducting funds if activity cannot be verified.
 */
contract FitnessUnbreakableVow is WeeklyGoalsRecords, Ownable, PhysicalActivityOracleListener {
    /**
     * @dev Oracle responsinble for storing physical activity records.
     */
    Oracle public immutable PHYSICAL_ACTIVITY_ORACLE;

    /**
     * @dev Chainlink Upkeep address. Upkeep is responsible for calling this contract at least once a day.
     */
    address public immutable CHAINLINK_UPKEEP_ADDRESS;

    /**
     * @dev Giveth Charity Wallet.
     * @dev https://giveth.io/project/Giveth-Matching-Pool-0
     */
    address public constant GIVETH_WALLET_ADDRESS = 0x6e8873085530406995170Da467010565968C7C62;

    /**
     * @notice The total amount of funds staked in this contract at deployment.
     * @dev This value is set during contract construction and represents the initial funds received.
     */
    uint256 public immutable STAKED_AMOUNT;

    /**
     * @notice The penalty amount deducted for each failed week.
     * @dev Calculated at contract deployment based on the total staked amount and contract duration.
     * This value remains constant throughout the contract's lifetime.
     */
    uint256 public immutable PENALTY_AMOUNT;

    constructor(
        address physicalActivityOracleAddress,
        address upkeepAddress,
        uint256 creationDate,
        uint256 expirationDate
    ) payable WeeklyGoalsRecords(creationDate, expirationDate) {
        STAKED_AMOUNT = msg.value;
        CHAINLINK_UPKEEP_ADDRESS = upkeepAddress;
        PHYSICAL_ACTIVITY_ORACLE = Oracle(physicalActivityOracleAddress);
        PENALTY_AMOUNT = STAKED_AMOUNT / ((EXPIRATION_DATE - CREATION_DATE) / SECONDS_IN_A_WEEK);

        requireSameDatesAsOracle(CREATION_DATE, EXPIRATION_DATE);
    }

    /**
     * @notice Checks if physical activity goals have been achieved. If not,
     * a portion of the contract's funds is transferred to the caller as a penalty.
     * @dev This function incentivizes meeting activity goals by imposing a financial penalty
     * for non-compliance, rewarding the caller who verifies the unmet goal.
     */
    function enforceAgreement() external notExpired {
        updateAllWeekStatuses();

        int8 weekNumber = findFirstWeekPendingPenalty();

        if (weekNumber != -1) {
            applyPenaltyForWeek(uint8(weekNumber));
        } else {
            emit NoPenaltyApplied();
        }
    }

    /**
     * @notice Transfers all remaining contract funds to the owner after contract (vow) has expired.
     * Can only be called after the contract has expired and by the contract's owner.
     */
    function terminateVow() external onlyOwner isExpired {
        uint256 balance = address(this).balance;

        require(balance > 0, "No funds to release");

        payable(owner).transfer(balance);

        emit VowExpired(balance, msg.sender);
    }

    function onNewPhysicalActivityRecord(uint8 weekIndex, PhysicalActivityRecord memory record) external onlyOracle {
        putWeek(weekIndex, buildWeeklyGoalsFrom(record));
    }

    /** 
     * @notice Retrieves all weekly goals completion records.
     * @return An array of `WeeklyGoalsCompletionResult` structs containing the completion records for all weekly goals.
     */
    function getAllWeeklyGoalsRecords() external view returns (WeeklyGoalsCompletionResult[] memory) {
        return listAllWeeks();
    }

    receive() external payable {}

    function applyPenaltyForWeek(uint8 weekIndex) private {
        weeklyGoalsRecords[weekIndex].status = WeeklyGoalStatus.FAILED_PENALTY_APPLIED;

        uint256 penaltyAmount = calculatePenaltyAmount();

        if(isBeingCalledByUpkeep()) {
            sendPenaltyToCharity(penaltyAmount);
        } else {
            sendPenaltyToCaller(penaltyAmount);
        }
    }

    function sendPenaltyToCaller(uint256 penaltyAmount) private {
        payable(msg.sender).transfer(penaltyAmount);

        emit PenaltyApplied(penaltyAmount, msg.sender);
    }

    function sendPenaltyToCharity(uint256 penaltyAmount) private {
        payable(GIVETH_WALLET_ADDRESS).transfer(penaltyAmount);

        emit PenaltyApplied(penaltyAmount, GIVETH_WALLET_ADDRESS);
    }

    function calculatePenaltyAmount() private view returns (uint256) {
        if (address(this).balance < PENALTY_AMOUNT) return 0;

        return PENALTY_AMOUNT;
    }

    function isBeingCalledByUpkeep() private view returns (bool) {
        return msg.sender == CHAINLINK_UPKEEP_ADDRESS;
    }

    function requireSameDatesAsOracle(uint256 creationDate, uint256 expirationDate) internal view {
        require(PHYSICAL_ACTIVITY_ORACLE.CREATION_DATE() == creationDate, "Oracle and Vow creation dates diverge.");
        require(PHYSICAL_ACTIVITY_ORACLE.EXPIRATION_DATE() == expirationDate, "Oracle and Vow expiration dates diverge.");
    }

    modifier onlyOracle() {
        require(address(PHYSICAL_ACTIVITY_ORACLE) == msg.sender, "Callable only by the oracle.");
        _;
    }
}