import { spawn } from 'child_process';
import { BytesLike } from 'ethers';


export default async function verify(
    network: string,
    label: string,
    address: string,
    constructorArguments: Array<string | number | bigint | BytesLike>,
    timeoutMs = 45_000,
) {
    return;

    const args = [
        'hardhat',
        'verify',
        '--network',
        network,
        address,
        ...constructorArguments.map((arg) => arg.toString()),
    ];

    const cmd = process.platform === 'win32' ? 'npx.cmd' : 'npx';

    console.log(`Running verification for ${label}: ${cmd} ${args.join(' ')}`);

    await new Promise<void>((resolve) => {
        const child = spawn(cmd, args, { stdio: 'inherit', shell: true });

        const timer = setTimeout(() => {
            console.warn(`Verification for ${label} timed out after ${timeoutMs}ms. Killing process.`);
            child.kill();
            resolve();
        }, timeoutMs);

        child.on('exit', (code) => {
            clearTimeout(timer);
            if (code === 0) {
                console.log(`Verification for ${label} succeeded.`);
            } else {
                console.warn(`Verification for ${label} exited with code ${code}.`);
            }
            resolve();
        });

        child.on('error', (err) => {
            clearTimeout(timer);
            console.error(`Failed to start verification for ${label}:`, err);
            resolve();
        });
    });
}
