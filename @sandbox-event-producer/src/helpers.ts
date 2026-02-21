import { Contract } from "ethers";

export function requireEnv(name: string): string {
    const value = process.env[name];

    if (!value) throw new Error(`Missing environment variable: ${name}`);

    return value;
}

export async function getSignerAddress(contract: Contract) {
    const runner = contract.runner as { getAddress?: () => Promise<string>; address?: string } | null;

    if (runner?.getAddress) return await runner.getAddress();
    if (runner?.address) return runner.address;

    throw new Error('Unable to resolve signer address for reset.');
}

export async function getCurrentWeekWindow(timeLord: Contract) {
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

export async function getVowNewTimeline(timeLord: Contract, secondsInWeekOverride?: number, numberOfWeeksOverride?: number) {
    const [secondsInWeekRaw, numberOfWeeksRaw]: [bigint, bigint] = await Promise.all([timeLord.SECONDS_IN_ONE_WEEK(), timeLord.NUMBER_OF_WEEKS()]);

    const secondsInWeek = Number(secondsInWeekOverride !== undefined ? secondsInWeekOverride : secondsInWeekRaw);
    const numberOfWeeks = Number(numberOfWeeksOverride !== undefined ? numberOfWeeksOverride : numberOfWeeksRaw);

    const creationDate = Math.floor((Date.now() / 1000) + (secondsInWeek / 2));
    const expirationDate = creationDate + secondsInWeek * numberOfWeeks;

    return { creationDate, expirationDate, secondsInWeek };
}

export async function getScheduleNewTimeline(timeLord: Contract) {
    const [creationDate, expirationDate, secondsInWeek] = await Promise.all([
        timeLord.CREATION_DATE(),
        timeLord.EXPIRATION_DATE(),
        timeLord.SECONDS_IN_ONE_WEEK(),
    ]);

    const secondsInWeekNumber = Number(secondsInWeek);
    const rateMinutes = Math.max(1, Math.floor(secondsInWeekNumber / 60));
    const intervalMs = rateMinutes * 60_000;

    const computedStartMs = (Number(creationDate) + secondsInWeekNumber / 2) * 1000;
    const minStartMs = Date.now() + 60_000;
    const startMs = Math.max(computedStartMs, minStartMs);

    let endMs = Number(expirationDate + secondsInWeek + 10n) * 1000;
    if (endMs <= startMs + intervalMs) {
        endMs = startMs + intervalMs + 60_000;
    }

    const startDate = new Date(startMs);
    const endDate = new Date(endMs);
    const schedulerExpression = `rate(${rateMinutes} minutes)`

    return { startDate, endDate, schedulerExpression };
}
