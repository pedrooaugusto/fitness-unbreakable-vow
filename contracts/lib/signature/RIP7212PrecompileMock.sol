// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { P256 } from "@openzeppelin/contracts/utils/cryptography/P256.sol";

/**
 * @dev RIP-7212 precompile shim for local dev. Accepts the same calldata layout
 * as the Arbitrum precompile (h, r, s, qx, qy) and returns a single word bool.
 */
contract RIP7212PrecompileMock {
    error InvalidCalldataLength(uint256 received);

    fallback(bytes calldata data) external returns (bytes memory) {
        if (data.length != 0xa0) {
            revert InvalidCalldataLength(data.length);
        }

        bytes32 h;
        bytes32 r;
        bytes32 s;
        bytes32 qx;
        bytes32 qy;

        assembly {
            h := calldataload(0x00)
            r := calldataload(0x20)
            s := calldataload(0x40)
            qx := calldataload(0x60)
            qy := calldataload(0x80)
        }

        bool isValid = P256.verifySolidity(h, r, s, qx, qy);
        return abi.encode(isValid);
    }
}
