/**
 * Minifies the Deno script `signature-verifier-script.ts`
 * and appends to the file `SignatureVerifierScript.sol`.
 *
 * The idea is that minifying this DenoJS script the contract
 * will use less gas when deployed.
 */
import terser from 'terser';
import path from 'path';
import fs from 'fs';

export default function minifySignatureVerifierSourceCode() {
    const workingDir = path.resolve(__dirname, '..', 'contracts', 'lib');

    const sourceCode = fs.readFileSync(path.resolve(workingDir, 'signature-verifier-script.ts'), 'utf-8');

    const output = terser.minify_sync(sourceCode, {
        module: false,
        compress: {},
        mangle: {},
        output: {},
        parse: {},
    });

    // Unwraps function. Eg: ` function(){ hello() } ---> hello() `
    // Thats how Chainlink Functions expects them
    const minifiedSourceCode = output.code?.substring(22, output.code.length - 1)!;

    const solScript = fs
        .readFileSync(path.resolve(workingDir, 'SignatureVerifierScript.sol'), 'utf-8')
        .replace(/(constant SOURCE_CODE2 = ').*?(')/, `$1${minifiedSourceCode}$2`);

    fs.writeFileSync(path.resolve(workingDir, 'SignatureVerifierScript.sol'), solScript);
}
