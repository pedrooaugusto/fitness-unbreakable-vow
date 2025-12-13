// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { WeeklyGoalStatus, WeeklyGoal, PhysicalActivityStats, Listener, OracleInterface, Environment } from './lib/Types.sol';
import { TimeLord, TimeBound } from './lib/timelord/Types.sol';
import { Ownable } from './lib/Ownable.sol';
import { Versioned } from './lib/Versioned.sol';
import { UpkeeperManager } from './lib/UpkeeperManager.sol';
import { console } from './lib/variants/console.sol';
import { WeeklyGoalListable } from './lib/WeeklyGoalListable.sol';

event NoPenaltyApplied();
event PenaltyApplied(uint8 weekIndex, address enforcer);
event VowTeminated(uint256 releasedFunds, address receiver);

interface ArbSys { function arbBlockNumber() external view returns (uint256); }

/**
 * @title FitnessUnbreakableVow: Penalizes Physical Inactivity with Fund Deduction.
 * @notice This contract enforces physical activity goals by deducting funds if activity cannot be verified.
 */
contract FitnessUnbreakableVow is WeeklyGoalListable, UpkeeperManager, Ownable, Listener, Versioned, TimeBound {
    /**
     * @dev Oracle responsinble for storing physical activity records.
     */
    OracleInterface public immutable PHYSICAL_ACTIVITY_ORACLE;
    TimeLord public immutable TIME_LORD;

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
        OracleInterface physicalActivityOracleAddress
    ) payable WeeklyGoalListable(physicalActivityOracleAddress.TIME_LORD()) {
        PHYSICAL_ACTIVITY_ORACLE = physicalActivityOracleAddress;
        TIME_LORD = PHYSICAL_ACTIVITY_ORACLE.TIME_LORD();
        STAKED_AMOUNT = msg.value;
        PENALTY_AMOUNT = STAKED_AMOUNT / ((TIME_LORD.EXPIRATION_DATE() - TIME_LORD.CREATION_DATE()) / TIME_LORD.SECONDS_IN_ONE_WEEK());

        PHYSICAL_ACTIVITY_ORACLE.registerPhysicalActivityStatsUpdateListener(address(this));

        if (Environment.isLocalhost()) return;

        _createUpkeeper(TIME_LORD.END_OF_WEEK_CRON());
    }

    /**
     * @notice Checks if physical activity goals have been achieved. If not,
     * a portion of the contract's funds is transferred to the caller as a penalty.
     * @dev This function incentivizes meeting activity goals by imposing a financial penalty
     * for non-compliance, rewarding the caller who verifies the unmet goal.
     * Callable by anyone; scans weeks in order and applies the first missing-goal penalty.
     */
    function enforceAgreement() external onlyBeforeFullExpiry {
        int8 weekIndex = findFirstFailedWeek();

        if (weekIndex != -1) {
            applyPenaltyForWeek(uint8(weekIndex));
        } else {
            emit NoPenaltyApplied();
        }
    }

    /**
     * @notice Transfers remaining contract funds to the owner after the vow expires or pulls LINK from Upkeep.
     * @dev If `withdrawUpkeeperFunds2` is true, it only withdraws LINK from the Upkeep and returns early.
     * Otherwise, it releases the ETH balance to the owner, cancels the Upkeep, and emits `VowTeminated`.
     */
    function terminateAgreement(bool withdrawUpkeeperFunds2) external onlyOwner {
        // Allow vow termination if public keys weren't registered.
        require(isPublicKeyNotSet() || TIME_LORD.isContractFullyExpired(), "Contract has not expired yet.");

        if (withdrawUpkeeperFunds2) {
            _withdrawUpkeeperFunds();

            return;
        }

        uint256 balance = address(this).balance;

        require(balance > 0, "No funds to release");

        payable(owner).transfer(balance);

        _cancelUpkeeper();

        emit VowTeminated(balance, msg.sender);
    }

    /**
     * @notice Callback invoked by the oracle when weekly physical activity stats change.
     * @dev Updates internal weekly goal records based on the latest stats for `weekIndex`.
     */
    function onPhysicalActivityStatsUpdate(uint8 weekIndex, PhysicalActivityStats calldata stats) external onlyOracle {
        putWeek(weekIndex, buildWeeklyGoalFrom(stats));
    }

    /**
     * @notice One-time configuration of the Chainlink Upkeeper used to automate enforcement.
     * @dev Owner sets the Upkeeper address and funds it with LINK using the provided amount.
     */
    function configureUpkeeper(address upkeeper, uint256 linkFunding) external onlyOwner {
        require(address(CHAINLINK_UPKEEPER_ADDRESS) == address(0), "Upkeeper already set.");

        _configureUpkeeper(upkeeper, linkFunding, TIME_LORD.END_OF_WEEK_CRON());
    }

    /** 
     * @notice Retrieves all weekly goals completion records.
     * @return An array of `WeeklyGoal` structs containing the completion records for all weekly goals.
     */
    function getAllWeeklyGoalsRecords() external view returns (WeeklyGoal[] memory) {
        return listAllWeeks();
    }

    receive() external payable {}

    function applyPenaltyForWeek(uint8 weekIndex) private {
        weeklyGoalsRecords[weekIndex].penaltyBlock = getTransactionBlock();

        uint256 penaltyAmount = calculatePenaltyAmount();

        if(isBeingCalledByUpkeep()) {
            weeklyGoalsRecords[weekIndex].status = WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UPKEEPER;

            _sendEth(GIVETH_WALLET_ADDRESS, penaltyAmount);
        } else {
            weeklyGoalsRecords[weekIndex].status = WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UNKOWN;

            _sendEth(GIVETH_WALLET_ADDRESS, penaltyAmount / 2);
            payable(msg.sender).transfer(penaltyAmount / 2);
        }

        emit PenaltyApplied(weekIndex, msg.sender);
    }

    function calculatePenaltyAmount() private view returns (uint256) {
        if (address(this).balance < PENALTY_AMOUNT) return 0;

        return PENALTY_AMOUNT;
    }

    function isBeingCalledByUpkeep() private view returns (bool) {
        return msg.sender == address(CHAINLINK_UPKEEPER_ADDRESS);
    }

    function isPublicKeyNotSet() private view returns (bool) {
        return !PHYSICAL_ACTIVITY_ORACLE.isPublicKeySet();
    }

    function getTransactionBlock() private view returns (uint256) {
        if (Environment.isLocalhost()) return block.number;

        return ArbSys(address(100)).arbBlockNumber();
    }

    function _sendEth(address to, uint256 amount) private {
        (bool success, ) = to.call{value: amount}("");

        require(success, "ETH transfer failed");
    }

    modifier onlyOracle() {
        require(address(PHYSICAL_ACTIVITY_ORACLE) == msg.sender, "Callable only by the oracle.");
        _;
    }

    modifier onlyBeforeFullExpiry() {
        require(!TIME_LORD.isContractFullyExpired(), "Contract has expired.");
        _;
    }
}
