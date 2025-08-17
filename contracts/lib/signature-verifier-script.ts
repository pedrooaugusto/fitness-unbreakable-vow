/**
 * NodeJS script used to verify a given piece of data
 * was signed with the given public key. Since this piece
 * of code is deployed in the blockchain along with the
 * smart contract it must be small and efficent.
 * 
 * This code is evetually minified by the scripts/build.ts script.
 */
async function main() {
    const { webcrypto } = await import('node:crypto');
    const { Buffer } = await import('node:buffer');

    const PUBLIC_KEY = args[0];
    const ECDSA = { name: 'ECDSA', namedCurve: 'P-256' };
    const SignEcdsaAlgorithm = { name: 'ECDSA', hash: 'SHA-256' };
    const pad64 = value => Number(value).toString(16).padStart(64, '0');

    const signature = Buffer.from(args[1], 'base64');
    const data = args.slice(2).map(n => parseInt(n));

    const key = await webcrypto.subtle.importKey('raw', Buffer.from(PUBLIC_KEY, 'base64'), ECDSA, true, ['verify']);
    const verified = await webcrypto.subtle.verify(SignEcdsaAlgorithm, key, signature, new Uint32Array(data));

    const encoded64 = [verified, ...data].map(pad64).join('');

    return Buffer.from(encoded64, 'hex');
}