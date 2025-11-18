import { type Currency, type Network } from "../main/types";

export function formatCurrency(
    value?: number,
    currency: Currency = "usd"
) {
    if (value === undefined || value === null) return "-";

    if (currency === "eth") return `${value} ETH`;

    const formatter = new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: currency === "usd" ? "USD" : "BRL",
    });

    return formatter.format(value);
}

export function timeRemaining(secondsToExpire: number) {
    if (secondsToExpire <= 0) return "EXPIRED";

    return formatTime(secondsToExpire, " ");
}

export function formatTime(secondsToExpire: number, separator = " ", mode: 'short' | 'long' = 'short') {
    const days = Math.floor(secondsToExpire / (60 * 60 * 24));
    const hours = Math.floor((secondsToExpire % (60 * 60 * 24)) / (60 * 60));
    const minutes = Math.floor((secondsToExpire % (60 * 60)) / 60);
    const seconds = Math.floor(secondsToExpire % 60);

    const units = {
        long: [' days', ' hours', ' minutes', ' seconds'],
        short: ['d', 'h', 'm', 's']
    };

    const result = [];
    if (days > 0) result.push(`${days}${units[mode][0]}`);
    if (hours > 0) result.push(`${hours}${units[mode][1]}`);
    if (minutes > 0) result.push(`${minutes}${units[mode][2]}`);
    if (seconds > 0) result.push(`${seconds}${units[mode][3]}`);

    if (result.length === 0) return '0 minutes';

    return result.join(separator);
}

export function formatDate(
    timestamp: number,
    year: "numeric" | "2-digit" | null = "numeric",
    month: "2-digit" | "short" = "short"
) {
    const date = new Date(timestamp * 1000);

    return date.toLocaleDateString("en-US", {
        year: year == null ? undefined : year,
        month: month,
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    });
}

const blockExplorers = {
    sepolia: 'https://sepolia.etherscan.io/',
    arbitrum: 'https://arbiscan.io/',
    localhost: 'http://localhost:3000/',
    arbiSep: 'https://sepolia.arbiscan.io/',
}

export function getTransactionBlockExplorerUrl(tx: string, network: Network) {
    return blockExplorers[network] + 'tx' + '/' + tx;
}

export function getAddressBlockExplorerUrl(address: string, network: Network) {
    return blockExplorers[network] + 'address' + '/' + address;
}

export function shortAddress(address: string) {
    return address.substring(0, 6) + "..." + address.substring(address.length - 4);
}

export function getWeekStardAndEndDate(contractStartDate: number, currentWeekNumber: number, secondsInAWeek: number) {
    const weekStartDate = contractStartDate + currentWeekNumber * secondsInAWeek;
    const weekEndDate = weekStartDate + secondsInAWeek;

    return { weekStartDate, weekEndDate };
}

export const GIVETH_PAGE_URL = 'https://giveth.io/project/Giveth-Matching-Pool-0?tab=donations'