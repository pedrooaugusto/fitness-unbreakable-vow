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

const jsscript = fs.readFileSync(path.resolve('contracts', 'lib', 'signature-verifier-script.ts'), 'utf-8');
const output = terser.minify_sync(jsscript, {
    module: false,
    compress: {},
    mangle: {},
    output: {},
    parse: {},
});

const minifiedCode = output.code?.substring(22, output.code.length - 1)!;

let solScript = fs.readFileSync(path.resolve('contracts', 'lib', 'SignatureVerifierScript.sol'), 'utf-8');

solScript = solScript.replace(/(constant SOURCE_CODE2 = ').*?(')/, `$1${minifiedCode}$2`);

fs.writeFileSync(path.resolve('contracts', 'lib', 'SignatureVerifierScript.sol'), solScript);
