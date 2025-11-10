// SPDX-License-Identifier: MIT 
pragma solidity ^0.8.28;

interface IExpirable {
    function EXPIRATION_DATE() external view returns(uint256);
    function CREATION_DATE() external view returns(uint256);
    function SECONDS_IN_ONE_WEEK() external view returns(uint256);
}

enum ContractPhase { Active, Grace, FullyExpired }

function min256(uint256 a, uint256 b) pure returns (uint256) { return a > b ? b : a; }

abstract contract Expirable is IExpirable {
    uint256 public immutable SECONDS_IN_ONE_WEEK;
    uint256 public immutable CREATION_DATE;
    uint256 public immutable EXPIRATION_DATE;
    uint256 public immutable GRACE_PERIOD;
    uint8 public immutable NUMBER_OF_WEEKS;

    constructor(uint256 creationDate, uint256 expirationDate, uint256 secondsInOneWeek) {
        SECONDS_IN_ONE_WEEK = secondsInOneWeek;
        GRACE_PERIOD = min256(uint256(secondsInOneWeek / 5), 3600);
        NUMBER_OF_WEEKS = uint8((expirationDate - creationDate) / SECONDS_IN_ONE_WEEK);
        CREATION_DATE = creationDate;
        EXPIRATION_DATE = creationDate + NUMBER_OF_WEEKS * SECONDS_IN_ONE_WEEK; // Force multiple of
    }

    modifier onlyWhileActive() {
        require(isContractActive(), "Contract has expired.");
        _;
    }

    modifier onlyBeforeFullExpiry() {
        require(!isContractFullyExpired(), "Contract has expired.");
        _;
    }

    modifier onlyAfterFullExpiry() {
        require(isContractFullyExpired(), "Contract has not expired yet.");
        _;
    }

    function getContractPhase() public view returns (ContractPhase) {
        if (isContractFullyExpired()) return ContractPhase.FullyExpired;

        if (isContractActive()) return ContractPhase.Active;

        return ContractPhase.Grace;
    }

    function getCurrentWeekIndex() public view returns (uint8) {
        return getWeekIndexOf(block.timestamp);
    }

    function getWeekIndexOf(uint256 timestamp) internal view returns (uint8) {
        require(timestamp >= CREATION_DATE, "!! Wibbly Wobbly Timey Wimey !!");

        if (timestamp >= EXPIRATION_DATE) return NUMBER_OF_WEEKS - 1;

        uint8 weekIndex = uint8((timestamp - CREATION_DATE) / SECONDS_IN_ONE_WEEK);

        return weekIndex;
    }

    function isContractActive() internal view returns (bool) {
        return block.timestamp < EXPIRATION_DATE;
    }

    function isContractInGracePeriod() internal view returns (bool) {
        return block.timestamp >= EXPIRATION_DATE && block.timestamp < EXPIRATION_DATE + GRACE_PERIOD;
    }

    function isContractFullyExpired() internal view returns (bool) {
        return block.timestamp >= EXPIRATION_DATE + GRACE_PERIOD;
    }
}