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

export async function getScheduleWindow(timeLord: Contract) {
    const [creationDate, expirationDate, secondsInWeek] = await Promise.all([
        timeLord.CREATION_DATE(),
        timeLord.EXPIRATION_DATE(),
        timeLord.SECONDS_IN_ONE_WEEK(),
    ]);

    const halfWeek = Math.floor(Number(secondsInWeek) / 2);
    const startDate = new Date((Number(creationDate) + halfWeek) * 1000);
    const endDate = new Date(Number(expirationDate + secondsInWeek + 10n) * 1000);

    return { startDate, endDate };
}
