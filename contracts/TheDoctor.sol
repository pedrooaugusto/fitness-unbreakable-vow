// SPDX-License-Identifier: MIT 
pragma solidity ^0.8.28;

import { TimeLord, ContractPhase } from './lib/timelord/Types.sol';

function min256(uint256 a, uint256 b) pure returns (uint256) { return a > b ? b : a; }

/// @title The Doctor, the last of the Time Lords.
/// @author A TV Company.
/// @notice The laws of time are his to make it!
/// @dev https://www.youtube.com/watch?v=wXrqtC81ztA
contract TheDoctor is TimeLord {
    /// @notice Number of seconds that define one week for the schedule.
    uint256 public immutable SECONDS_IN_ONE_WEEK;
    /// @notice Unix timestamp (seconds) when the contract was created
    /// and the schedule starts.
    uint256 public immutable CREATION_DATE;
    /// @notice Unix timestamp (seconds) when the active phase ends
    /// and the grace period begins.
    uint256 public immutable EXPIRATION_DATE;
    /// @notice Length of the grace period (in seconds) after
    /// `EXPIRATION_DATE` before the contract is fully expired.
    uint256 public immutable GRACE_PERIOD;
    /// @notice Total number of whole weeks in the active phase
    /// (from creation until `EXPIRATION_DATE`).
    uint8 public immutable NUMBER_OF_WEEKS;

    constructor(uint256 creationDate, uint256 expirationDate, uint256 secondsInOneWeek) {
        SECONDS_IN_ONE_WEEK = secondsInOneWeek;
        GRACE_PERIOD = min256(uint256(secondsInOneWeek / 5), 3600);
        NUMBER_OF_WEEKS = uint8((expirationDate - creationDate) / SECONDS_IN_ONE_WEEK);
        CREATION_DATE = creationDate;
        EXPIRATION_DATE = creationDate + NUMBER_OF_WEEKS * SECONDS_IN_ONE_WEEK; // Force multiple of
    }

    function isContractActive() public view returns (bool) {
        return block.timestamp < EXPIRATION_DATE;
    }

    function isContractInGracePeriod() public view returns (bool) {
        return block.timestamp >= EXPIRATION_DATE && block.timestamp < EXPIRATION_DATE + GRACE_PERIOD;
    }

    function isContractFullyExpired() public view returns (bool) {
        return block.timestamp >= EXPIRATION_DATE + GRACE_PERIOD;
    }

    /// @notice Returns the current phase of the contract
    /// (Active, Grace, or FullyExpired).
    /// @return The current contract phase.
    function getContractPhase() public view returns (ContractPhase) {
        if (isContractFullyExpired()) return ContractPhase.FullyExpired;

        if (isContractActive()) return ContractPhase.Active;

        return ContractPhase.Grace;
    }

    /// @notice Returns the index of the current week in the active schedule.
    /// @dev Once past `EXPIRATION_DATE`, this value is clamped to the last
    /// week index (`NUMBER_OF_WEEKS - 1`). Week indices start at 0.
    /// @return The current week index starting at 0.
    function getCurrentWeekIndex() public view returns (uint8) {
        return getWeekIndexOf(block.timestamp);
    }

    function getWeekIndexOf(uint256 timestamp) public view returns (uint8) {
        require(timestamp >= CREATION_DATE, "!! Wibbly Wobbly Timey Wimey !!");

        if (timestamp >= EXPIRATION_DATE) return NUMBER_OF_WEEKS - 1;

        uint8 weekIndex = uint8((timestamp - CREATION_DATE) / SECONDS_IN_ONE_WEEK);

        return weekIndex;
    }
}