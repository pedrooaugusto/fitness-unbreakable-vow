import { ethers, JsonRpcSigner, type Eip1193Provider, type Eip6963ProviderInfo } from "ethers";
import React from "react";
import type { Network } from "../types";
import type { TransactionResponse } from "ethers";
import type { Contract } from "ethers";
import type { Log } from "ethers";
import type { TransactionReceipt } from "ethers";

interface Eip6963Announcement {
    type: "eip6963:announceProvider";
    detail: {
        info: Eip6963ProviderInfo;
        provider: Eip1193Provider;
    }
}

type Wallet = Eip6963Announcement['detail'] & {
    connected: boolean
}

export type ParsedTransactionReceipt = TransactionReceipt & { parsedLogs: ethers.LogDescription[] }

interface UseVowEnforcerResult {
    wallets: Wallet[];
    enforceVow: () => Promise<ParsedTransactionReceipt>;
    connectToWallet: (walletName: string) => Promise<void>;
}

const VOW_ABI = [
    "function enforceAgreement() public",
    "event NoPenaltyApplied()",
    "event PenaltyApplied(uint8 weekIndex, address enforcer)"
];

export function useVowEnforcer(targetNetwork: Network, vowAddress: string): UseVowEnforcerResult {
    const [wallets, setWallets] = React.useState<Wallet[]>([]);
    const [selectedSigner, setSelectedSigner] = React.useState<JsonRpcSigner | null>(null);

    const enforceVow = async () => {
        try {
            const contract = new ethers.Contract(vowAddress, VOW_ABI, selectedSigner);
            const tx = await contract.enforceAgreement() as TransactionResponse;

            console.log("Transaction sent:", tx.hash);

            const transactionReceipt = await tx.wait() as ParsedTransactionReceipt

            transactionReceipt.parsedLogs = parseLogs(contract, transactionReceipt?.logs as unknown as ethers.Log[]);

            console.log("Transaction confirmed!");
            return transactionReceipt;
        } catch (err) {
            console.error("Contract call failed:", err);

            throw err;
        }
    }

    const connectToWallet = async (walletName: string) => {
        const wallet = wallets.find(({ info }) => info.name === walletName);

        if (wallet == null) throw new Error('Wallet not found.');

        await wallet.provider.request({ method: 'eth_requestAccounts' });

        const targetNetworkChainId = '0x' + NetworkChainIdMap[targetNetwork].toString(16);

        await wallet.provider.request({ method: "wallet_switchEthereumChain", params: [{ chainId: targetNetworkChainId }]});

        const ethersProvider = new ethers.BrowserProvider(wallet.provider);
        const userSigner = await ethersProvider.getSigner();

        setSelectedSigner(userSigner);
        // TODO Multiple wallets bug
        setWallets(wallets.map(thisWallet => ({ ...thisWallet, connected: thisWallet.info.name === walletName })));

        console.log("Connected to:", walletName);
    }

    const hasFetched = React.useRef(false);
    React.useEffect(() => {
        if (hasFetched.current) return;

        hasFetched.current = true;

        discoverWallets()
            .then((walletsFound) => {
                setWallets(walletsFound);
            }).catch((err) => {
                alert('Error when discovering wallets. ' + (err || '').toString());
            });
    }, []);

    return {
        wallets,
        connectToWallet,
        enforceVow
    }
}

export const discoverWallets = (): Promise<Wallet[]> => {
    return new Promise((resolve) => {
        const providers: Wallet[] = [];

        const handler = (event: Eip6963Announcement) => {
            if (event.detail.provider == null) return;

            providers.push({ ...event.detail, connected: false });
        };

        window.addEventListener("eip6963:announceProvider", handler as unknown as EventListener);
        window.dispatchEvent(new Event("eip6963:requestProvider"));

        console.log(providers);

        setTimeout(() => {
            window.removeEventListener("eip6963:announceProvider", handler as unknown as EventListener);
            resolve(providers);
        }, 1000);
    });
};


const NetworkChainIdMap: Record<number | Network, Network | number> = { 
    42161: 'arbitrum',
    'arbitrum': 42161,
    11155111: 'sepolia',
    'sepolia': 11155111,
    31337: 'localhost',
    'localhost': 31337,
    'arbiSep': 421614,
    421614: 'arbiSep'
};

const parseLogs = (contract: Contract, logs: Log[] = []) => {
    const parsedLogs = [];

    for (const log of logs) {
        try {
            const parsed = contract.interface.parseLog(log);

            if (parsed == null) continue;

            parsedLogs.push(parsed);
        } catch {
            // Skip it
        }
    }

    return parsedLogs;
}
