// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

enum ContractPhase { Active, Grace, FullyExpired }

interface TimeLord {
    /// @notice Unix timestamp (seconds) when the active phase ends
    /// and the grace period begins.
    /// @return Unix timestamp in seconds.
    function EXPIRATION_DATE() external view returns(uint256);
    /// @notice Unix timestamp (seconds) when the contract was created
    /// and the schedule starts.
    /// @return Unix timestamp in seconds.
    function CREATION_DATE() external view returns(uint256);
    /// @notice Number of seconds that define one week for the schedule.
    /// @return Number of seconds in one week.
    function SECONDS_IN_ONE_WEEK() external view returns(uint256);
    /// @notice Length of the grace period (in seconds) after
    /// `EXPIRATION_DATE` before the contract is fully expired.
    function GRACE_PERIOD() external view returns(uint256);
    /// @notice If you need to call this contract at the end
    /// every weekly term use this cron expression.
    function END_OF_WEEK_CRON() external view returns(string memory);

    function reset(uint256 creationDate, uint256 expirationDate) external;
    function isContractActive() external view returns (bool);
    function isContractInGracePeriod() external view returns (bool);
    function isContractFullyExpired() external view returns (bool);
    function getContractPhase() external view returns (ContractPhase);
    function getCurrentWeekIndex() external view returns (uint8);
    function getWeekIndexOf(uint256 timestamp) external view returns (uint8);
}

interface TimeBound {
    function TIME_LORD() external view returns(TimeLord);
}