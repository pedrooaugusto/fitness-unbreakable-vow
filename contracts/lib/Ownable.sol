// SPDX-License-Identifier: MIT 
pragma solidity ^0.8.28;

abstract contract Ownable {
    /**
     * @notice The address of the contract owner, set once at deployment and cannot be changed.
     * @dev This variable is marked as `immutable`, meaning it is assigned during contract construction and remains constant thereafter.
     */
    address public immutable owner;

    constructor() {
        owner = msg.sender;
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "Ownable: caller is not the owner");
        _;
    }

    modifier onlyOwnerOrigin() {
        require(tx.origin == owner, "Forbidden.");
        _;
    }
}