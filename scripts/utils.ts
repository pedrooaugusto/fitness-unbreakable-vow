import { RunningEventStruct } from "../typechain-types/contracts/PhysicalActivityOracle";
import { sign, verify } from "./keys";
import { HardhatRuntimeEnvironment } from "hardhat/types/runtime";

export async function signPhysicalActivityRecord(record: Omit<RunningEventStruct, 'signature'>) {
    const buffer = Buffer.alloc(20);

    buffer.writeUInt32BE(5 >> 0, 0);
    buffer.writeUInt32BE(record.timestamp.valueOf() as number >>> 0, 4);
    buffer.writeUInt32BE(record.distanceInMeters.valueOf() as number >>> 0, 8);
    buffer.writeUInt32BE(record.paceInSecondsPerKm.valueOf() as number >>> 0, 12);
    buffer.writeUInt32BE(record.avgBpm.valueOf() as number >>> 0, 16);

    const signature = await sign(buffer);

    return { signature, data: buffer, signedRecord: { ...record, signature: signature } };
}

export async function test() {
    const { signature, data } = await signPhysicalActivityRecord({ 
        timestamp: 1000,
        runDistanceMeters: 1200,
        healthySleepNights: 0,
        gymVisits: 0,
    });

    const sig = Buffer.from(signature, 'base64')
    const rawKeyArrayBuffer = sig.buffer.slice(sig.byteOffset, sig.byteOffset + sig.byteLength);

    const result = await verify(rawKeyArrayBuffer, data);

    console.log(result);
}

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

