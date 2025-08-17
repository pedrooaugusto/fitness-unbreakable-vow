import { PhysicalActivityRecordStruct } from "../typechain-types/contracts/PhysicalActivityOracle";
import { sign, verify } from "./keys";

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