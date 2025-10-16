import path from 'path';
import fs from 'fs';
import { execSync } from 'child_process';

export default function setContractVersion() {
    const repoRoot = path.resolve(__dirname, '..');

    const commit = execSync('git rev-parse HEAD', { cwd: repoRoot }).toString().trim();

    const tag = execSync('git tag --points-at HEAD', { cwd: repoRoot }).toString().trim().split(/\r?\n/).filter(Boolean)[0] || '';

    const commitLink = tag
        ? `https://github.com/pedrooaugusto/fitness-unbreakable-vow/releases/tag/${tag}`
        : `https://github.com/pedrooaugusto/fitness-unbreakable-vow/commit/${commit}`;

    const workingDir = path.resolve(repoRoot, 'contracts', 'lib');
    const versionedPath = path.resolve(workingDir, 'Versioned.sol');
    const solScript = fs
        .readFileSync(versionedPath, 'utf-8')
        .replace(/(string public constant VERSION = ').*?(')/, `$1${commitLink}$2`);

    fs.writeFileSync(versionedPath, solScript);
}
