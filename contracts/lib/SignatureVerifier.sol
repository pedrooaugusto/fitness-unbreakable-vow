// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { FunctionsClient } from "@chainlink/contracts/src/v0.8/functions/v1_0_0/FunctionsClient.sol";
import { FunctionsRequest } from "@chainlink/contracts/src/v0.8/functions/v1_0_0/libraries/FunctionsRequest.sol";
import { PhysicalActivityRecord } from "./Types.sol";
import { Uint32ToString } from "./Strings.sol";
import { ChainLinkFunctionsParamsProvider, ChainLinkFunctionsParams, ChainlinkFunctionsMockLib, console } from "./Config.sol";
import { SignatureVerifierScript } from "./SignatureVerifierScript.sol";

/**
 * @title Physical Record Signature Verifier
 * @author pedroaus
 * @notice Provides a framework for verifying the authenticity of physical activity records
 * using Chainlink Functions.
 */
abstract contract SignatureVerifier is FunctionsClient {
    using FunctionsRequest for FunctionsRequest.Request;

    ChainLinkFunctionsParams private chainlinkParams;
    bytes32 private s_lastRequestId;

    /**
     * @notice Indicates whether the public key has been set. Prevents overwriting
     * the key after initial assignment.
     */
    bool public publicKeySet = false;

    /**
     * @notice Stores the base64-encoded public key used for signature verification.
     * Can only be set once. The oracle will only accept physical activity records
     * signed with the corresponding private key.
     */
    string public BASE64_PUBLIC_KEY = "";

    constructor(ChainLinkFunctionsParams memory chainlinkFnParams) FunctionsClient(chainlinkFnParams.router) {
        chainlinkParams = chainlinkFnParams;
    }

    /**
     * @notice Initiates the signature verification process by creating and sending a Chainlink Functions request.
     * @param signature The cryptographic signature to be verified.
     * @param record The physical activity record associated with the signature.
     */
    function verifySignature(string calldata signature, PhysicalActivityRecord calldata record) internal {
        FunctionsRequest.Request memory request = createRequest(signature, record);

        s_lastRequestId = sendRequest(request);
    }

    /**
     * @notice Called when the signature verification process is complete.
     * @param error Indicates if there was an error during verification.
     * @param verified Indicates if the signature was successfully verified.
     * @param record The physical activity record associated with the verification.
     */
    function signatureVerificationComplete(
        bool error,
        bool verified,
        PhysicalActivityRecord memory record
    ) internal virtual;

    /**
     * @notice Sets the public key to be used in the signature verification.
     * Can only be called once. Once this is set, the contract is forever
     * tied with the private key and will only accept requests from
     * such key.
     * @param publicKey The base64-encoded public key to set.
     */
    function setPublicKey(string calldata publicKey) external {
        require(!publicKeySet, "Public key already set");
        require(bytes(publicKey).length > 0, "Public key cannot be empty");

        BASE64_PUBLIC_KEY = publicKey;
        // publicKeySet = true;
    }

    function fulfillRequest(bytes32 requestId, bytes memory response, bytes memory err) internal override {
        console.log("[PhysicalActivityOracle] Received signature verification response form chainlink.");

        if (s_lastRequestId != requestId) {
            revert("UnexpectedRequestID");
        }

        if (err.length != 0) {
            signatureVerificationComplete(true, false, PhysicalActivityRecord(0, 0, 0, 0));
        } else {
            console.log("[PhysicalActivityOracle] Decoding signature verification response");

            (
                bool sourceIsVerified,
                uint32 timestamp,
                uint16 runDistanceMeters,
                uint8 healthySleepNights,
                uint8 gymVisits
            ) = abi.decode(response, (bool, uint32, uint16, uint8, uint8));

            PhysicalActivityRecord memory record = PhysicalActivityRecord(timestamp, runDistanceMeters, healthySleepNights, gymVisits);

            signatureVerificationComplete(false, sourceIsVerified, record);
        }
    }

    function createRequest(string calldata signature, PhysicalActivityRecord calldata record) private view returns (FunctionsRequest.Request memory) {        

        FunctionsRequest.Request memory request;

        // Check source code to understand how the signature is verified
        request.initializeRequestForInlineJavaScript(SignatureVerifierScript.SOURCE_CODE2);

        string[] memory args = new string[](6);

        args[0] = BASE64_PUBLIC_KEY;
        args[1] = signature;
        args[2] = Uint32ToString.toString(record.timestamp);
        args[3] = Uint32ToString.toString(record.runDistanceMeters);
        args[4] = Uint32ToString.toString(record.healthySleepNights);
        args[5] = Uint32ToString.toString(record.gymVisits);

        request.setArgs(args);

        return request;
    }

    function sendRequest(FunctionsRequest.Request memory request) private returns (bytes32) {
        console.log("[PhysicalActivityOracle] Calling chainlink oracle to verify signature.");

        // DEBUG ONLY
        if (chainlinkParams.networkName == ChainLinkFunctionsParamsProvider.HARDHAT_NETWORK_HASH) {
            return ChainlinkFunctionsMockLib.executeCode(address(i_router), request.source, request.args);
        } else {
            return _sendRequest(request.encodeCBOR(), chainlinkParams.subscriptionId, chainlinkParams.gasLimit, chainlinkParams.donId);
        }
    }
}