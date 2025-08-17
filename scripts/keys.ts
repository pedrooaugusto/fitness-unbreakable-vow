import { webcrypto } from 'node:crypto';

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

export const PUBLIC_KEY = "BDkVzRT59TGuG9bZ9awy6bwXJJrA6fpWIXTJTl2cPWg6dDDDWWu/lOW0ZpOLYCy7HQEyw4Otdh6PCjSAhJFyeZ0=";

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

export async function sign(data: Uint32Array) {
    const privateKey = await getPrivatekey();

    return await webcrypto.subtle.sign(SignEcdsaAlgorithm, privateKey, data);
}

export async function verify(signature: ArrayBuffer, data: Uint32Array) {
    const key = await getPublickey();

    return await webcrypto.subtle.verify(SignEcdsaAlgorithm, key, signature, data);
}
