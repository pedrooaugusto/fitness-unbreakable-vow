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

    const days = Math.floor(secondsToExpire / (60 * 60 * 24));
    const hours = Math.floor((secondsToExpire % (60 * 60 * 24)) / (60 * 60));
    const minutes = Math.floor((secondsToExpire % (60 * 60)) / 60);

    const result = [];
    if (days > 0) result.push(`${days}d`);
    if (hours > 0) result.push(`${hours}h`);
    if (minutes > 0) result.push(`${minutes}min`);

    return result.length > 0 ? result.join(" ") : "a few seconds";
}

export function formatDate(
    timestamp: number,
    year: "numeric" | "2-digit" | null = "numeric"
) {
    const date = new Date(timestamp * 1000);

    return date.toLocaleDateString("en-US", {
        year: year == null ? undefined : year,
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    });
}

const blockExplorers = {
    sepolia: 'https://sepolia.etherscan.io/',
    arbitrum: 'https://arbiscan.io/',
    localhost: 'http://localhost:3000/'
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

export const GIVETH_PAGE_URL = 'https://giveth.io/project/Giveth-Matching-Pool-0?tab=donations'

export const API_HOST = `${location.protocol}//${location.hostname}:3000/api`;