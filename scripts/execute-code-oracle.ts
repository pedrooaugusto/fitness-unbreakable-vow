import path from 'path';
import fs from 'fs';
import { exec } from 'child_process';
import { promisify } from 'util';

const execPromise = promisify(exec);

const MOCK_FOLDER = path.join(__dirname, '..', 'artifacts', 'mock');

const enrichCodeToExecute = (code: string) => {
    return `
        async function executeCode() {
            const args: any[] = Deno.args;

            ${code}
        }

        async function main() {
            const result = await executeCode();

            await Deno.writeFile('artifacts/mock/output.bin', result, { create: true });
        }

        main()
    `;
}

const buildRunDenoScriptCommand = (scriptPath: string, scriptArgs: string[]) => {
    return `deno run \
        --allow-net \
        --allow-read \
        --allow-write \
        --node-modules-dir=none \
        --allow-env \
        ${scriptPath} ` + scriptArgs.join(" ");
};

export async function executeCode(code: string, scriptArgs: string[]): Promise<Uint8Array> {
    const fileToExecutePath = path.join(MOCK_FOLDER, 'code.ts');
    const fileContent = enrichCodeToExecute(code);

    fs.mkdirSync(MOCK_FOLDER, { recursive: true });
    fs.writeFileSync(fileToExecutePath, fileContent);

    const { stdout, stderr } = await execPromise(buildRunDenoScriptCommand(fileToExecutePath, scriptArgs));

    //console.log(`Deno script stderr:\n${stderr}`);

    //console.log(`Deno script stdout: \n${stdout}`);

    return await fs.promises.readFile(path.join(MOCK_FOLDER, 'output.bin'));
}