import * as dotenv from 'dotenv';
import { ScheduledHandler } from 'aws-lambda';
import { loadContracts } from './contract';
import { createRandomPhysicalActivityRequest } from './physical-activity';
import { Contract, JsonRpcProvider } from 'ethers';
import { getCurrentWeekWindow, getScheduleNewTimeline, getVowNewTimeline, getSignerAddress } from './helpers';
import { syncEventBridgeSchedule } from './scheduler';

dotenv.config();

type SandboxEvent = {
    contractAddress: string;
    network: string;
    secondsInWeekOverride?: number;
    numberOfWeeksOverride?: number;
};

export const handler: ScheduledHandler<SandboxEvent> = (async (event: SandboxEvent) => {
    const { timeLord, fitnessUnbreakableVow, physicalActivityOracle, provider } = await loadContracts(event.contractAddress, event.network);

    const isActive = await timeLord.isContractActive();

    if (!isActive || event.secondsInWeekOverride) {
        console.log('[INFO] Contract is not active. Resetting sandbox and syncing event bridge scheduler.');

        await resetVow(timeLord, fitnessUnbreakableVow, provider, event.secondsInWeekOverride, event.numberOfWeeksOverride);

        await sleep(3000);

        const scheduleWindow = await getScheduleNewTimeline(timeLord);

        await syncEventBridgeSchedule({
            startDate: scheduleWindow.startDate,
            endDate: scheduleWindow.endDate,
            scheduleExpression: scheduleWindow.schedulerExpression,
            inputOverrides: {
                contractAddress: event.contractAddress,
                network: event.network,
            },
        });

        return;
    }

    const { weekStart, secondsInWeek, currentWeekIndex } = await getCurrentWeekWindow(timeLord);

    const request = await createRandomPhysicalActivityRequest({
        countPerType: 2,
        weekStart,
        secondsInWeek,
        physicalActivityOracle
    });

    console.log(`[INFO] Publishing fake events for week #${currentWeekIndex}.`);
    console.log(`[INFO] Events:`, request)

    const tx = await physicalActivityOracle.publishPhysicalActivityEvent(request);
    await tx.wait();

    console.log(`[INFO] Publish completed. tx=${tx.hash}`);

    if (Math.random() >= 0.7) { // 30% chance of happening
        console.log(`[INFO] Enforcing vow.`);

        await sleep(3000);

        const tx1 = await fitnessUnbreakableVow.enforceAgreement();
        await tx1.wait();

        console.log(`[INFO] Enforce completed. tx=${tx1.hash}`);
    }

}) as any;

async function resetVow(timeLord: Contract, vow: Contract, provider: JsonRpcProvider, secondsInWeekOverride?: number, numberOfWeeksOverride?: number) {
    const { creationDate, expirationDate, secondsInWeek } = await getVowNewTimeline(timeLord, secondsInWeekOverride, numberOfWeeksOverride)

    const stakedAmount = await vow.STAKED_AMOUNT();
    const vowBalance = await provider.getBalance(await vow.getAddress());
    const missingAmount = stakedAmount > vowBalance ? (stakedAmount - vowBalance) : 0n;

    if (missingAmount > 0n) {
        const signerAddress = await getSignerAddress(vow);
        const signerBalance = await provider.getBalance(signerAddress);
        const requiredBalance = (missingAmount * 6n) / 5n;

        if (signerBalance < requiredBalance) {
            throw new Error(`Insufficient funds to reset. Missing=${missingAmount.toString()}, Required=${requiredBalance.toString()}`);
        }
    }

    const tx = missingAmount > 0n
        ? await vow.reset(creationDate, expirationDate, secondsInWeek, { value: missingAmount })
        : await vow.reset(creationDate, expirationDate, secondsInWeek);

    await tx.wait();

    console.log(`[INFO] Sandbox reset. start=${creationDate}, end=${expirationDate}, missing=${missingAmount.toString()}, tx=${tx.hash}`);
}

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// handler({ "contractAddress": "0xeC87F28a65bF7DC2c87768E5a9136f00C3b2a539", "network": "arbiSep" } as any, null as any, null as any);
