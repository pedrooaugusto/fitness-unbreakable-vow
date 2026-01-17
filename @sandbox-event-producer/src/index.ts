import * as dotenv from 'dotenv';
import { ScheduledHandler } from 'aws-lambda';
import { loadContracts } from './contract';
import { createRandomPhysicalActivityRequest } from './physical-activity';
import { Contract } from 'ethers';

dotenv.config();

type SandboxEvent = {
    contractAddress: string;
    network: string;
};

export const handler: ScheduledHandler<SandboxEvent> = (async (event: SandboxEvent) => {
    const { timeLord, fitnessUnbreakableVow, physicalActivityOracle, provider } = await loadContracts(event.contractAddress, event.network);

    const isActive = await timeLord.isContractActive();

    if (!isActive) {
        console.log('[INFO] Contract is not active. Resetting sandbox.');

        await resetVow(timeLord, fitnessUnbreakableVow, provider);

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
}) as any;

async function getCurrentWeekWindow(timeLord: Contract) {
    const [creationDate, secondsInWeek, currentWeekIndex] = await Promise.all([
        timeLord.CREATION_DATE(),
        timeLord.SECONDS_IN_ONE_WEEK(),
        timeLord.getCurrentWeekIndex(),
    ]);

    const weekStart = Number(creationDate) + Number(secondsInWeek) * Number(currentWeekIndex);

    return {
        weekStart,
        secondsInWeek: Number(secondsInWeek),
        currentWeekIndex: Number(currentWeekIndex),
    };
}

async function resetVow(timeLord: Contract, vow: Contract, provider: import('ethers').JsonRpcProvider) {
    const [secondsInWeek, numberOfWeeks] = await Promise.all([timeLord.SECONDS_IN_ONE_WEEK(), timeLord.NUMBER_OF_WEEKS()]);

    const creationDate = Math.floor(Date.now() / 1000);
    const expirationDate = creationDate + Number(secondsInWeek) * Number(numberOfWeeks);

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
        ? await vow.reset(creationDate, expirationDate, { value: missingAmount })
        : await vow.reset(creationDate, expirationDate);

    await tx.wait();

    console.log(`[INFO] Sandbox reset. start=${creationDate}, end=${expirationDate}, missing=${missingAmount.toString()}, tx=${tx.hash}`);
}

async function getSignerAddress(contract: Contract) {
    const runner = contract.runner as { getAddress?: () => Promise<string>; address?: string } | null;

    if (runner?.getAddress) return await runner.getAddress();
    if (runner?.address) return runner.address;

    throw new Error('Unable to resolve signer address for reset.');
}

// handler({ contractAddress: '0x20969B53bCfce53Df3A04651A54Ef9E1bEf50b25', network: 'localhost' } as any, null as any, null as any);
