import { useEffect, useState } from "react";
import type { ReactElement } from "react";
import { timeRemaining } from "../../utils";

type LiveTimeCountdownProps = {
    endDate: Date;
    className?: string;
};

const FALLBACK_TEXT = "-";

export default function LiveTimeCountdown({ endDate, className }: LiveTimeCountdownProps): ReactElement {
    const [remainingText, setRemainingText] = useState<string>(() => formatRemaining(resolveEndDate(endDate)));

    useEffect(() => {
        const targetTimestamp = resolveEndDate(endDate);

        if (targetTimestamp === null) {
            setRemainingText(FALLBACK_TEXT);

            return;
        }

        const updateCountdown = () => {
            setRemainingText(formatRemaining(targetTimestamp));
        };

        updateCountdown();

        const intervalId = window.setInterval(updateCountdown, 1000);

        return () => window.clearInterval(intervalId);
    }, [endDate]);

    return <span className={className}>{remainingText}</span>;
}

function resolveEndDate(endDate: LiveTimeCountdownProps["endDate"]): number | null {
    const timestamp = endDate.getTime();

    return Number.isNaN(timestamp) ? null : timestamp;
}

function formatRemaining(targetTimestamp: number | null): string {
    if (targetTimestamp === null) return FALLBACK_TEXT;

    const secondsLeft = Math.floor((targetTimestamp - Date.now()) / 1000);

    return timeRemaining(secondsLeft);
}
