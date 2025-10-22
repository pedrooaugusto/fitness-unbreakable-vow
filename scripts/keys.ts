import { webcrypto } from 'node:crypto';
import { bigintTo32Bytes, bytesToBigInt } from './utils';

// Look at p-256 implementation to understand where it comes from.
const N_HEX = "0xFFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551";
const P256_N = BigInt(N_HEX);
const P256_HALF_N = P256_N >> 1n;

const ECDSA = { name: "ECDSA", namedCurve: "P-256" };
const SignEcdsaAlgorithm = { name: "ECDSA", hash: "SHA-256" };

/**
 * Used for testing only, no security breach here.
 */
const PRIVATE_KEY = {
    key_ops: [ 'sign' ],
    ext: true,
    kty: 'EC',
    x: 'ORXNFPn1Ma4b1tn1rDLpvBckmsDp-lYhdMlOXZw9aDo',
    y: 'dDDDWWu_lOW0ZpOLYCy7HQEyw4Otdh6PCjSAhJFyeZ0',
    crv: 'P-256',
    d: 'GWwTF3f3mAYjrHQQi3tP04D5FR32-R-g0wugyXECbYk'
}

const PUBLIC_KEY = "BDkVzRT59TGuG9bZ9awy6bwXJJrA6fpWIXTJTl2cPWg6dDDDWWu/lOW0ZpOLYCy7HQEyw4Otdh6PCjSAhJFyeZ0=";

export async function generateKeys() {
    const keypair = await webcrypto.subtle.generateKey(ECDSA, true, [
        "sign",
        "verify",
    ]);

    const publicKey = await webcrypto.subtle.exportKey("raw", keypair.publicKey);
    const privateKey = await webcrypto.subtle.exportKey("jwk", keypair.privateKey);

    console.log(privateKey);
    console.log(Buffer.from(publicKey).toString('base64'));
}

async function getPrivatekey() {
    const privatekey = await webcrypto.subtle.importKey("jwk", PRIVATE_KEY, ECDSA, true, ["sign"]);

    return privatekey;
}

async function getPublickey() {
    const publicKey = await webcrypto.subtle.importKey("raw", Buffer.from(PUBLIC_KEY, 'base64'), ECDSA, true, ["verify"]);

    return publicKey;
}

export async function getRawPublicKey() {
    const rawPublicKey = new Uint8Array(await webcrypto.subtle.exportKey("raw", await getPublickey()));

    return { x: rawPublicKey.slice(1, 33), y: rawPublicKey.slice(33, 65) };
}

export async function sign(data: Buffer<ArrayBuffer>, options: { normalizeLowS: boolean } = { normalizeLowS: true }) {
    const privateKey = await getPrivatekey();

    const signature = await webcrypto.subtle.sign(SignEcdsaAlgorithm, privateKey, data);

    const signatureBytes = new Uint8Array(signature);

    const r = signatureBytes.slice(0, 32) as Uint8Array;
    const s = signatureBytes.slice(32, 64) as Uint8Array;

    if (options.normalizeLowS) {
        const sBigInt = bytesToBigInt(s);

        const normalizedS = sBigInt > P256_HALF_N ? (P256_N - sBigInt) : sBigInt;

        return { r, s: bigintTo32Bytes(normalizedS) }
    }

    return { r, s }
}

export async function verify(signature: ArrayBuffer, data: Uint32Array) {
    const key = await getPublickey();

    return await webcrypto.subtle.verify(SignEcdsaAlgorithm, key, signature, data);
}
