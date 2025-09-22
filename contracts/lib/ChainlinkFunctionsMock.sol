// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { console } from "hardhat/console.sol";

interface ChainLinkCaller {
    function handleOracleFulfillment(bytes32 requestId, bytes memory response, bytes memory err) external;
}

event ExcuteCodeRequest(string[] codeToExecuteArgs);
event ExcuteCodeResponse(bool sourceIsVerified, uint32 timestamp, uint16 runDistanceMeters, uint8 healthySleepNights, uint8 gymVisits);

/**
 * @title A version of of Chainlink Functions. This contract can receive arbitrary
 * requests to execute code.
 * @author pedroaus
 * @notice Used for testing only.
 */
contract ChainlinkFunctionsMock {
    string private codeToExecute;
    string[] private codeToExecuteArgs;
    uint256 private requestId = 0;
    ChainLinkCaller private callerAddress;

    function executeCode(string memory code, string[] memory args) external returns (bytes32) {
        console.log("Received Request to executing code.");
        codeToExecute = code;
        codeToExecuteArgs = args;
        callerAddress = ChainLinkCaller(msg.sender);
        requestId += 1;

        emit ExcuteCodeRequest(codeToExecuteArgs);

        return bytes32(requestId);
    }

    function hasCodeToExecute() external view returns (bool) {
        return bytes(codeToExecute).length != 0;
    }
    
    function getCodeToExecute() external view returns (string memory) {
        return codeToExecute;
    }

    function getCodeToExecuteArgs() external view returns (string[] memory) {
        return codeToExecuteArgs;
    }

    function setCodeToExecuteResponse(bytes memory data) external {
        ChainLinkCaller callerAddressCopy = callerAddress;

        emptyCodeToExecute();

        (bool sourceIsVerified, uint32 timestamp, uint16 runDistanceMeters, uint8 healthySleepNights, uint8 gymVisits) = abi.decode(data, (bool, uint32, uint16, uint8, uint8));

        emit ExcuteCodeResponse(sourceIsVerified, timestamp, runDistanceMeters, healthySleepNights, gymVisits);

        try callerAddressCopy.handleOracleFulfillment(bytes32(requestId), data, bytes("")) {
        } catch Error(string memory errorMessage) {
            console.log("Failed to fullfill request: ", errorMessage);
        }
    }

    function emptyCodeToExecute() private {
        callerAddress = ChainLinkCaller(payable(0));
        codeToExecute = "";
    }
}