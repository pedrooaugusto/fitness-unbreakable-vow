import { webcrypto } from 'node:crypto';

// Look at p-256 implementation in scripts/keys.ts for the constants and format.
const N_HEX = "0xFFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551";
const P256_N = BigInt(N_HEX);
const P256_HALF_N = P256_N >> 1n;

const ECDSA = { name: "ECDSA", namedCurve: "P-256" } as const;
const SignEcdsaAlgorithm = { name: "ECDSA", hash: "SHA-256" } as const;

/**
 * Sandbox key for signing fake events.
 * The oracle public key must match this private key.
 */
const PRIVATE_KEY = {
    key_ops: [ 'sign' ],
    ext: true,
    kty: 'EC',
    x: 'ORXNFPn1Ma4b1tn1rDLpvBckmsDp-lYhdMlOXZw9aDo',
    y: 'dDDDWWu_lOW0ZpOLYCy7HQEyw4Otdh6PCjSAhJFyeZ0',
    crv: 'P-256',
    d: 'GWwTF3f3mAYjrHQQi3tP04D5FR32-R-g0wugyXECbYk'
};

export type P256Signature = { r: Uint8Array; s: Uint8Array };

export async function signP256(data: Buffer, options: { normalizeLowS: boolean } = { normalizeLowS: true }): Promise<P256Signature> {
    const privateKey = await getPrivateKey();
    const signature = await webcrypto.subtle.sign(SignEcdsaAlgorithm, privateKey, data);
    const signatureBytes = new Uint8Array(signature);

    const r = signatureBytes.slice(0, 32) as Uint8Array;
    const s = signatureBytes.slice(32, 64) as Uint8Array;

    if (!options.normalizeLowS) {
        return { r, s };
    }

    const sBigInt = bytesToBigInt(s);
    const normalizedS = sBigInt > P256_HALF_N ? (P256_N - sBigInt) : sBigInt;

    return { r, s: bigintTo32Bytes(normalizedS) };
}

async function getPrivateKey() {
    return await webcrypto.subtle.importKey("jwk", PRIVATE_KEY, ECDSA, true, ["sign"]);
}

function bigintTo32Bytes(value: bigint): Uint8Array {
    const bytes: number[] = [];

    while (value > 0n) {
        bytes.push(Number(value & 0xffn));
        value >>= 8n;
    }

    const out = new Uint8Array(32);
    const start = 32 - bytes.length;

    for (let i = 0; i < bytes.length; i++) {
        out[start + i] = bytes[bytes.length - 1 - i];
    }

    return out;
}

function bytesToBigInt(arr: Uint8Array): bigint {
    let res = 0n;

    for (const byte of arr) {
        res = (res << 8n) + BigInt(byte);
    }

    return res;
}
