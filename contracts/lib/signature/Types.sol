// SPDX-License-Identifier: MIT
pragma solidity ^0.8.28;

struct P256Signature {
    bytes32 r;
    bytes32 s;
}

struct P256PublicKey {
    bytes32 x;
    bytes32 y;
}

struct AndroidKeyAttestation {
    string attestationSha256;
    string attestationChallenge;
    string attestationIpfsCID;
}

interface SignatureVerifier {
    function isPublicKeySet() external view returns (bool);
}