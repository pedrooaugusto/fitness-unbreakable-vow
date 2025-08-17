pragma solidity ^0.8.28;

/**
 * @title Deno code to verify a ECDSA signature.
 * @notice AUTO-GENERATED. Check real source code at ./signature-verifier-script.ts
 */
library SignatureVerifierScript {
    string internal constant SOURCE_CODE2 = 'const{webcrypto:a}=await import("node:crypto"),{Buffer:r}=await import("node:buffer"),e=args[0],t=r.from(args[1],"base64"),n=args.slice(2).map((a=>parseInt(a))),i=await a.subtle.importKey("raw",r.from(e,"base64"),{name:"ECDSA",namedCurve:"P-256"},!0,["verify"]),o=[await a.subtle.verify({name:"ECDSA",hash:"SHA-256"},i,t,new Uint32Array(n)),...n].map((a=>Number(a).toString(16).padStart(64,"0"))).join("");return r.from(o,"hex")';
}