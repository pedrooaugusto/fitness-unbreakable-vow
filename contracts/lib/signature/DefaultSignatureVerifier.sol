// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

import { P256PublicKey, P256Signature, AndroidKeyAttestation, SignatureVerifier } from './Types.sol';
import { console } from '../variants/console.sol';

/**
 * @title Physical Record Signature Verifier
 * @author pedroaus
 * @notice Provides a framework for verifying the authenticity of physical activity records using P-256 signatures.
 * @dev Uses RIP-7212 precompile to verify secp256r1 signatures.
 */
abstract contract DefaultSignatureVerifier is SignatureVerifier {
    /**
     * @notice Stores the raw x and y values of the public key used for signature verification.
     * @dev Can only be set once. The oracle will only accept physical activity records signed with the corresponding private key.
     */
    P256PublicKey public PUBLIC_KEY;

    /**
     * @notice Captures metadata proving the origin of `PUBLIC_KEY` via Android Key Attestation.
     * @dev Stores the attestation SHA-256, the attestation challenge, and an IPFS CID pointing
     * to the attestation certificate chain. Meant for off-chain verification to confirm the key
     * was generated and held in a device-backed Keystore and that Google issued the attestation.
     */
    AndroidKeyAttestation public PUBLIC_KEY_ATTESTATION;

    function verifySignature(P256Signature calldata signature, bytes32 sha256Data) internal view onlyIfPublicKeyIsSet returns(bool) {
        // Android hashes the message and then signs it; on-chain we hash the same message and feed the digest to _rip7212 (P256).
        return _rip7212(
            sha256Data,
            signature.r,
            signature.s,
            PUBLIC_KEY.x,
            PUBLIC_KEY.y
        );
    }

    /**
     * @notice Sets the public key used for P-256 signature verification and records its Android Key Attestation.
     * @dev After setting, the oracle is intended to accept only records signed by the corresponding private key.
     * @param publicKey The raw `x` and `y` coordinates of the P-256 public key.
     * @param keyAttestation Android Key Attestation metadata (digest, challenge, and IPFS CID of the cert chain).
     */
    function _setPublicKey(P256PublicKey calldata publicKey, AndroidKeyAttestation calldata keyAttestation) internal {
        require(PUBLIC_KEY.x == bytes32(0), "Public key already set");
        require(PUBLIC_KEY.y == bytes32(0), "Public key already set");
        require(publicKey.x != bytes32(0), "Public key cannot be empty");
        require(publicKey.y != bytes32(0), "Public key cannot be empty");

        PUBLIC_KEY = publicKey;
        // Rather than use Chainlink to validate this, I will leave it up to you.
        // Use the IPFS CID to download the attestation certificate and check if
        // it was issued by Google.
        PUBLIC_KEY_ATTESTATION = keyAttestation;
    }

    function _emergencyPublicKeyChange(
        P256PublicKey calldata newPublicKey,
        AndroidKeyAttestation calldata newKeyAttestation
    ) internal {
        require(newPublicKey.x != bytes32(0), "Public key cannot be empty");
        require(newPublicKey.y != bytes32(0), "Public key cannot be empty");

        PUBLIC_KEY = newPublicKey;
        PUBLIC_KEY_ATTESTATION = newKeyAttestation;
    }

    /**
     * @dev Taken as it is from: `@openzeppelin/contracts/utils/cryptography/P256.sol`
     * 
     * Removed a few signature and public key unnecessary checks.
     */
    function _rip7212(bytes32 h, bytes32 r, bytes32 s, bytes32 qx, bytes32 qy) private view returns (bool isValid) {
        assembly ("memory-safe") {
            // Use the free memory pointer without updating it at the end of the function
            let ptr := mload(0x40)
            mstore(ptr, h)
            mstore(add(ptr, 0x20), r)
            mstore(add(ptr, 0x40), s)
            mstore(add(ptr, 0x60), qx)
            mstore(add(ptr, 0x80), qy)
            // RIP-7212 precompiles return empty bytes when an invalid signature is passed, making it impossible
            // to distinguish the presence of the precompile. Custom precompile implementations may decide to
            // return `bytes32(0)` (i.e. false) without developers noticing, so we decide to evaluate the return value
            // without expanding memory using scratch space.
            mstore(0x00, 0) // zero out scratch space in case the precompile doesn't return anything
            if iszero(staticcall(gas(), 0x100, ptr, 0xa0, 0x00, 0x20)) {
                invalid()
            }
            isValid := mload(0x00)
        }
    }

    function isPublicKeySet() public view returns (bool) {
        return PUBLIC_KEY.x != bytes32(0) && PUBLIC_KEY.y != bytes32(0);
    }

    modifier onlyIfPublicKeyIsSet {
        require(isPublicKeySet(), 'Public key not set.');
        _;
    }
}
