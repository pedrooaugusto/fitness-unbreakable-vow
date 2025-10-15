import type { LogDescription } from "ethers";
import type { Network } from "../types";
import { useVowEnforcer } from "./useVowEnforcer";

interface WalletButtonProps {
    network: Network;
    vowAddress: string;
    onEnforceVowStart: () => void;
    onEnforceVowSuccess: (transaction: string, events: LogDescription[]) => void;
    onEnforceVowFail: (error: string) => void;
}

export default function CallContractButton({ network, vowAddress, onEnforceVowFail, onEnforceVowSuccess, onEnforceVowStart }: WalletButtonProps) {
    const { wallets, connectToWallet, enforceVow } = useVowEnforcer(network, vowAddress);

    const connect = async() => {
        try {
            await connectToWallet(selectedWallet.info.name);
        } catch (err) {
            alert('Unable to connect to wallet: ' + (err as Error).message);
            console.error(err);
        }
    }

    const enforce = async() => {
        onEnforceVowStart();
        try {
            const transaction = await enforceVow();
            onEnforceVowSuccess(transaction.hash, transaction.parsedLogs);
        } catch (err) {
            // alert('Unable to enforce vow: ' + (err as Error).message);
            onEnforceVowFail('Unable to enforce vow: ' + (err as Error).message);
            console.error(err);
        }
    }

    if (isMobile() && wallets.length === 0) {
        const metamaskLink = 'https://link.metamask.io/dapp/' + window.location.href;

        return (
            <div>
                <a href={metamaskLink}>
                    <button className="connect-to-wallet">
                        📱 View on Metamask
                    </button>
                </a>
            </div>
        )
    }

    if (wallets.length === 0) return null;

    const selectedWallet = wallets[0];

    return (
        <div>
            {!selectedWallet.connected ? (
                <button onClick={connect} className="connect-to-wallet">
                    🌐 Connect <b>Arbitrum</b> Wallet
                </button>
            ) : (
                <button onClick={enforce} className="connect-to-wallet connected">
                    📜⚖️ Enforce Vow
                </button>
            )}
        </div>
    );
}

function isMobile() {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
}