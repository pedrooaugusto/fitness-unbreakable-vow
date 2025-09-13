// SPDX-License-Identifier: MIT 
pragma solidity ^0.8.28;

import { Config } from './Config.sol';

interface IExpirable {
    function EXPIRATION_DATE() external view returns(uint256);
    function CREATION_DATE() external view returns(uint256);
}

abstract contract Expirable is IExpirable {
    uint256 public constant SECONDS_IN_A_WEEK = Config.SECONDS_IN_SEVEN_DAYS;

    uint256 public immutable CREATION_DATE;
    uint256 public immutable EXPIRATION_DATE;
    uint8 private immutable NUMBER_OF_WEEKS;

    constructor(uint256 creationDate, uint256 expirationDate) {
        NUMBER_OF_WEEKS = uint8((expirationDate - creationDate) / SECONDS_IN_A_WEEK);
        CREATION_DATE = creationDate;
        EXPIRATION_DATE = creationDate + NUMBER_OF_WEEKS * SECONDS_IN_A_WEEK; // Force multiple of
    }

    modifier notExpired() {
        require(!isContractExpired(), "Contract has expired.");
        _;
    }

    modifier isExpired() {
        require(isContractExpired(), "Contract has not expired yet.");
        _;
    }

    function isContractExpired() public view returns (bool) {
        return block.timestamp > EXPIRATION_DATE;
    }

    function getCurrentWeekIndex() public view returns (uint8) {
        return getWeekIndexOf(block.timestamp);
    }

    function getWeekIndexOf(uint256 timestamp) internal view returns (uint8) {
        require(timestamp >= CREATION_DATE, "!! Wibbly Wobbly Timey Wimey !!");

        if (timestamp >= EXPIRATION_DATE) return NUMBER_OF_WEEKS - 1;

        uint8 weekIndex = uint8((timestamp - CREATION_DATE) / SECONDS_IN_A_WEEK);

        // If expired, pretend it's the last week always
        //if (weekIndex >= NUMBER_OF_WEEKS) return NUMBER_OF_WEEKS - 1;

        return weekIndex;
    }
}