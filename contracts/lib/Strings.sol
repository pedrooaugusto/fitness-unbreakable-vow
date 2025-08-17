// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

library Uint32ToString {
    function toString(uint32 value) internal pure returns (string memory) {
        if (value == 0) {
            return "0";
        } else if (value == 1) {
            return "1";
        }

        uint32 temp = value;
        uint32 digits;
        while (temp != 0) {
            digits++;
            temp /= 10;
        }

        bytes memory buffer = new bytes(digits);
        while (value != 0) {
            digits -= 1;
            buffer[digits] = bytes1(uint8(48 + uint8(value % 10)));
            value /= 10;
        }

        return string(buffer);
    }
}