import { PhysicalActivityRecordStruct } from "../typechain-types/contracts/PhysicalActivityOracle";
import { sign, verify } from "./keys";
import { HardhatRuntimeEnvironment } from "hardhat/types/runtime";

export async function signPhysicalActivityRecord(record: PhysicalActivityRecordStruct) {
    const data = new Uint32Array([
        record.timestamp.valueOf() as number,
        record.runDistanceMeters.valueOf() as number,
        record.healthySleepNights.valueOf() as number,
        record.gymVisits.valueOf() as number
    ]);

    const signature = await sign(data);

    return { signature: Buffer.from(signature).toString('base64'), data };
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