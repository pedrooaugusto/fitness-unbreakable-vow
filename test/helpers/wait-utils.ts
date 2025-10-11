import { ContractTransactionResponse } from 'ethers';
import { PhysicalActivityOracle } from '../../typechain-types';
import { PhysicalActivityRecordStruct } from '../../typechain-types/contracts/PhysicalActivityOracle';
import { signPhysicalActivityRecord } from '../../scripts/utils';

export async function waitUntil(conditionFn: Function, timeoutMs = 5000, intervalMs = 50) {
    const start = Date.now();

    while (Date.now() - start < timeoutMs) {
        try {
            const result = await conditionFn();
            if (result) return; // Success!
            await new Promise(res => setTimeout(res, intervalMs));
        } catch(e) {
            console.error(e);
        }
    }

    throw new Error("Condition not met within timeout");
}

export async function noMoreCodeToExecute(fn: Function ) {
    await waitUntil(async () => (await fn(), 5000));
}

export async function pushPhysicalActivityRecord(contract: PhysicalActivityOracle, recordToAdd: PhysicalActivityRecordStruct, signature?: string) {
    signature = signature || (await signPhysicalActivityRecord(recordToAdd)).signature;

    const response = await contract.pushPhysicalActivityRecord(signature, recordToAdd);

    await response.wait();

    await waitUntil(async () => {
        const currentRecord = (await contract.getCurrentWeekPhysicalActivityRecord())[1];

        return currentRecord.timestamp == BigInt(recordToAdd.timestamp);
    }, 5000);
}

export async function confirmation(promise: Promise<ContractTransactionResponse>) {
    const transaction = await promise;

    return await transaction.wait();
}

