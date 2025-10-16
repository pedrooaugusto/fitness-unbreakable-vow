// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

library ChainLinkFunctionsParamsProvider {
    bytes32 internal constant ARBITRUM_NETWORK_HASH = keccak256(abi.encodePacked("ARBITRUM"));
    bytes32 internal constant SEPOLIA_NETWORK_HASH = keccak256(abi.encodePacked("SEPOLIA"));
    bytes32 internal constant HARDHAT_NETWORK_HASH = keccak256(abi.encodePacked("HARDHAT"));

    function get(string memory network) internal pure returns (ChainLinkFunctionsParams memory) {
        bytes32 networkHash = keccak256(abi.encodePacked(network));

        if (networkHash == ARBITRUM_NETWORK_HASH) {
            return ChainLinkFunctionsParams({
                router: 0x97083E831F8F0638855e2A515c90EdCF158DF238,
                donId: 0x66756e2d617262697472756d2d6d61696e6e65742d3100000000000000000000,
                subscriptionId: 49,
                gasLimit: 300_000,
                networkName: ARBITRUM_NETWORK_HASH
            });
        }

        if (networkHash == SEPOLIA_NETWORK_HASH) {
            return ChainLinkFunctionsParams({
                router: 0xb83E47C2bC239B3bf370bc41e1459A34b41238D0,
                donId: 0x66756e2d657468657265756d2d7365706f6c69612d3100000000000000000000,
                subscriptionId: 4835,
                gasLimit: 300_000,
                networkName: SEPOLIA_NETWORK_HASH
            });
        }

        // hardhat (localhost)
        return ChainLinkFunctionsParams({
            // Address of `ChainlinkFunctionsMock.sol`. Assumes the first contract
            // deployed to the hardhat network is the mock contract and therefore
            // will always have the first address available.
            router: 0x5FbDB2315678afecb367f032d93F642f64180aa3,
            donId: bytes32(0),
            subscriptionId: 0,
            gasLimit: 70000,
            networkName: HARDHAT_NETWORK_HASH
        });
    }
}

struct ChainLinkFunctionsParams {
    address router;
    bytes32 donId;
    uint64 subscriptionId;
    uint32 gasLimit;
    bytes32 networkName;
}