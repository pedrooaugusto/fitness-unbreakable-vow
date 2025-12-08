import * as Contracts from '../typechain-types';
import { HardhatRuntimeEnvironment, } from 'hardhat/types';
import { BytesLike, ContractTransactionReceipt, ethers } from 'ethers';
import { getContractAddress, saveContractAddress } from './addresses';
import { FitnessUnbreakableVow } from '../typechain-types';

export async function connectOrDeploy(address: string, name: string, hre: HardhatRuntimeEnvironment) {
    const deployedBytecode = await hre.ethers.provider.getCode(address);
    const artifact = await hre.artifacts.readArtifact(name);
    const Factory = await hre.ethers.getContractFactory(name);

    // Hardhat artifact has deployedBytecode (runtime code)
    if (deployedBytecode !== artifact.deployedBytecode) {
        console.log("Deploying new contract: " + name);

        const contract = await Factory.deploy();

        await contract.waitForDeployment();

        return contract;
    } else {
        console.log("Attaching to existing contract: " + name + ", " + address);

        return Factory.attach(address);
    }
}

export function bigintTo32Bytes(value: bigint): Uint8Array {
    const bytes: number[] = [];

    // bigint to byte array
    while (value > 0n) {
        bytes.push(Number(value & 0xffn));

        value >>= 8n;
    }

    // Padd with zeros
    const out = new Uint8Array(32);
    const start = 32 - bytes.length;

    for (let i = 0; i < bytes.length; i++) {
        out[start + i] = bytes[bytes.length - 1 - i];
    }

    return out;
}

export function bytesToBigInt(arr: Uint8Array): bigint {
    let res = 0n;
    
    for (const byte of arr) {
        res = (res << 8n) + BigInt(byte);
    }

    return res;
}

export function getTransactionEvent(eventId: string, eventDefinition: string, transaction: ContractTransactionReceipt | null) {
    if (transaction == null) return null;

    try {
        const iface = new ethers.Interface([eventDefinition]);
        const topic = ethers.id(eventId);
        for (const log of transaction.logs) {
            if (log.topics && log.topics[0] === topic) {
                return iface.decodeEventLog(eventId.slice(0, eventId.indexOf('(')), log.data, log.topics);
            }
        }
    } catch(err) {
        console.error('Unable to parse events', err);
    }

    return null;
}

export async function installRip7212Mock(hre: HardhatRuntimeEnvironment) {
    const RIP7212_PRECOMPILE_ADDRESS = '0x0000000000000000000000000000000000000100';
    const artifact = await hre.artifacts.readArtifact('RIP7212PrecompileMock');
    const bytecode = artifact.deployedBytecode;

    if (!bytecode || bytecode === '0x') {
        throw new Error('Missing RIP7212PrecompileMock bytecode. Run `npx hardhat compile` first.');
    }

    const currentCode = await hre.ethers.provider.getCode(RIP7212_PRECOMPILE_ADDRESS);

    if (currentCode === bytecode) return;

    await hre.network.provider.send('hardhat_setCode', [RIP7212_PRECOMPILE_ADDRESS, bytecode]);

    console.log(`Installed RIP-7212 mock precompile at ${RIP7212_PRECOMPILE_ADDRESS}`);
}

export interface LocalContractsMap {
    TheDoctor: [Contracts.TheDoctor__factory, Contracts.TheDoctor];
    PhysicalActivityOracle: [Contracts.PhysicalActivityOracle__factory, Contracts.PhysicalActivityOracle];
    FitnessUnbreakableVow: [Contracts.FitnessUnbreakableVow__factory, Contracts.FitnessUnbreakableVow];
}

export type LocalContracts = keyof LocalContractsMap;

export async function deployContract<ContractName extends LocalContracts>(
    hre: HardhatRuntimeEnvironment,
    name: ContractName,
    deployer: (factory: LocalContractsMap[ContractName][0]) => Promise<LocalContractsMap[ContractName][1]>
) {
    const contractFactory = await hre.ethers.getContractFactory(name) as LocalContractsMap[ContractName][0];
    const contract = await deployer(contractFactory);

    await contract.waitForDeployment();
    const contractAddress = await contract.getAddress();

    console.log(`${name} contract deployed to: ${contractAddress}`);

    saveContractAddress(name, contractAddress, hre.network.name, contract.deploymentTransaction()?.hash || '');

    return {
        contract,
        contractAddress
    }
}

export async function getContract<ContractName extends LocalContracts>(
    hre: HardhatRuntimeEnvironment,
    name: ContractName,
): Promise<LocalContractsMap[ContractName][1]> {
    const contractAddress = getContractAddress(name, hre.network.name);

    return await hre.ethers.getContractAt(name, contractAddress) as any as LocalContractsMap[ContractName][1];
}

export function to96BytesString(hre: HardhatRuntimeEnvironment, value: string): [BytesLike, BytesLike, BytesLike] {
    const valueBytes = hre.ethers.toUtf8Bytes(value);
    
    if (valueBytes.length > 96) {
        throw new Error(`value is too long (${valueBytes.length} bytes). Maximum supported is 96 bytes.`);
    }

    const paddedValue = new Uint8Array(96);
    paddedValue.set(valueBytes);

    return [
        hre.ethers.hexlify(paddedValue.slice(0, 32)) as BytesLike,
        hre.ethers.hexlify(paddedValue.slice(32, 64)) as BytesLike,
        hre.ethers.hexlify(paddedValue.slice(64, 96)) as BytesLike,
    ];
}

async function LINK(hre: HardhatRuntimeEnvironment) {
    const abi = [
        'function transfer(address,uint256) returns (bool)',
        'function approve(address,uint256) returns (bool)',
    ];
    const signers = await hre.ethers.getSigners();
    const address = hre.network.name === 'arbiSep' ? '0xb1D4538B4571d411F07960EF2838Ce337FE1E80E' : '0xf97f4df75117a78c1A5a0DBb814Af92458539FB4';

    return new hre.ethers.Contract(address, abi, signers[0]);
}

export class FitnessUnbreakableVowUpkeeper {
    private static cronCreatedEventId = 'NewCronUpkeepCreated(address,address)';
    private static cronCreatedEventDefinition = 'event NewCronUpkeepCreated(address upkeep, address owner)';

    static async create(contract: FitnessUnbreakableVow, hre: HardhatRuntimeEnvironment) {
        const deployTransaction = await contract.deploymentTransaction()?.wait()!!;
        const upkeepAddress = FitnessUnbreakableVowUpkeeper.getUpkeepAddress(deployTransaction);

        const initialFunding = hre.ethers.parseUnits('0.65', 18);
        await FitnessUnbreakableVowUpkeeper.fundUpkeepWithLink(contract, initialFunding, hre);

        const configTransaction = await contract.configureUpkeeper(upkeepAddress, initialFunding);
        await configTransaction.wait();
    }

    private static getUpkeepAddress(transaction: ContractTransactionReceipt | null) {
        const upkeepCreated = getTransactionEvent(
            FitnessUnbreakableVowUpkeeper.cronCreatedEventId,
            FitnessUnbreakableVowUpkeeper.cronCreatedEventDefinition,
            transaction
        ) as any;

        if (!upkeepCreated?.upkeep) throw Error('Unable to retrieve created upkeeper address.');

        return upkeepCreated.upkeep
    }

    private static async fundUpkeepWithLink(contract: FitnessUnbreakableVow, funds: BigInt, hre: HardhatRuntimeEnvironment) {
        const linkContract = await LINK(hre);

        await linkContract.approve(await contract.getAddress(), funds);
    }
}