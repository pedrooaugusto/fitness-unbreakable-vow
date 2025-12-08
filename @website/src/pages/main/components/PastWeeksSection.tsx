import React from "react";
import type { WithModalProps } from "../../components/modal";
import {
    WeeklyGoalStatus,
    type Currency,
    type GetContractOverviewResponse,
    type GetWeekDetailsResponse,
    type GymVisitEventValidator,
    type Network,
    type RunningEventValidator,
    type SleepEventValidator,
    type WeeklyGoal,
    type WeeklyGoalStatusType,
} from "../types";
import { SectionTitle } from "../../components/SectionTitle";
import PrizeIcon from "../../../assets/prize-icon";
import CheckCircleIcon from "../../../assets/check-circle-icon";
import XIcon from "../../../assets/x-circle-icon";
import DonateIcon from "../../../assets/donate-icon";
import {
    formatCurrency,
    formatDate,
    formatTime,
    getAddressBlockExplorerUrl,
    getTransactionBlockExplorerUrl,
    GIVETH_PAGE_URL,
    shortAddress,
} from "../../utils";
import CallContractButton from "./WalletButton";
import type { LogDescription } from "ethers";
import { getWeekDetails } from "../api/week-details";

interface PastWeeksSectionProps extends WithModalProps {
    overview: GetContractOverviewResponse;
    currency: Currency;
}

export const PastWeeksSection: React.FC<PastWeeksSectionProps> = ({
    overview,
    currency,
    openModal,
    closeModal,
}) => {
    const currentWeekNumber = overview.currentWeekNumber;
    const isContractExpired = overview.isContractExpired;
    const allWeeks = overview.allWeeks;
    const isEmpty = allWeeks.length === 0;

    return (
        <section className="past-weeks-results">
            <div className="content">
                <SectionTitle
                    icon={<PrizeIcon />}
                    text="Weekly Performance Summary"
                    subtext={
                        <>
                            This section summarizes the results of past weeks,
                            showing how many goals were met and the overall
                            status of each week.
                        </>
                    }
                />
                <div className="weeks-list">
                    {!isEmpty &&
                        allWeeks
                            .map((week, index) => {
                                // hide current week when the contract has not expired yet.
                                const hide = !isContractExpired && index === currentWeekNumber;

                                if (hide) {
                                    return null; // Skip weeks that are not over yet.
                                }

                                return (
                                    <PastWeekCard
                                        key={index}
                                        week={week}
                                        weekIndex={index}
                                        upkeeperAddress={overview.upkeeperAddress}
                                        openModal={openModal}
                                        closeModal={closeModal}
                                        network={overview.network}
                                        currency={currency}
                                        vowAddress={overview.contractAddress}
                                        penaltyAmount={overview.penaltyAmount}
                                        requiredNumberOfCompletedGoals={overview.requiredNumberOfCompletedGoals}
                                        gymVisitsGoal={overview.gymVisitsGoal}
                                        runningSessionsGoal={overview.runningSessionsGoal}
                                        healthySleepNightsGoal={overview.healthySleepNightsGoal}
                                        runningEventValidator={overview.runningValidator}
                                        sleepEventValidator={overview.sleepValidator}
                                        gymVisitEventValidator={overview.gymVisitValidator}
                                    />
                                );
                            })
                            .reverse()}
                </div>
            </div>
        </section>
    );
};

function PastWeekCard({
    week,
    weekIndex,
    requiredNumberOfCompletedGoals,
    gymVisitsGoal,
    runningSessionsGoal,
    healthySleepNightsGoal,
    runningEventValidator,
    gymVisitEventValidator,
    sleepEventValidator,
    openModal,
    network,
    upkeeperAddress,
    currency,
    penaltyAmount,
    vowAddress,
    closeModal,
}: {
    week: WeeklyGoal;
    weekIndex: number;
    network: Network;
    penaltyAmount: number;
    currency: Currency;
    vowAddress: string;
    requiredNumberOfCompletedGoals: number;
    gymVisitsGoal: number,
    runningSessionsGoal: number,
    healthySleepNightsGoal: number,
    runningEventValidator: RunningEventValidator,
    gymVisitEventValidator: GymVisitEventValidator,
    sleepEventValidator: SleepEventValidator,
    upkeeperAddress: string
} & WithModalProps) {
    const { goals, status } = week;

    const numberOfGoalsMet = [
        goals.gymVisitsGoalMet,
        goals.sleptWellGoalMet,
        goals.run2KmGoalMet,
    ].filter(Boolean).length;

    const isStatusPending = status === WeeklyGoalStatus.FAILED_PENDING_PENALTY;
    const isStatusFinal = status === WeeklyGoalStatus.COMPLETED ||
        status === WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UNKOWN ||
        status === WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UPKEEPER;
    const isStatusNull = status === WeeklyGoalStatus.NULL && 'Unclaimed!';

    const cardClass = () => {
        switch (status) {
        case WeeklyGoalStatus.COMPLETED: return "met";
        case WeeklyGoalStatus.FAILED_PENDING_PENALTY: return "not-met-pending-penalty";
        case WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UNKOWN: return "not-met";
        case WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UPKEEPER: return "not-met";
        default: return "not-met-unclaimed-penalty";
        }
    };

    const title = `📅 Week #${weekIndex} Summary`;
    const modal = (
        <PastWeekDetailsModal
            weekIndex={weekIndex}
            vowAddress={vowAddress}
            penaltyAmount={penaltyAmount}
            upkeeperAddress={upkeeperAddress}
            status={status}
            goals={week.goals}
            network={network}
            currency={currency}
            closeModal={closeModal}
            requiredNumberOfCompletedGoals={requiredNumberOfCompletedGoals}
            gymVisitsGoal={gymVisitsGoal}
            runningSessionsGoal={runningSessionsGoal}
            healthySleepNightsGoal={healthySleepNightsGoal}
            runningEventValidator={runningEventValidator}
            sleepEventValidator={sleepEventValidator}
            gymVisitEventValidator={gymVisitEventValidator}
        />
    );

    return (
        <div
            className={`week-card ${cardClass()}`}
            onClick={() => openModal(modal, title)}
        >
            <div className="title">Week {weekIndex}</div>
            <div className="value">
                {isStatusPending && "Claim Fine!"}
                {isStatusFinal && <>{numberOfGoalsMet}/{requiredNumberOfCompletedGoals} Goals</>}
                {isStatusNull && "Unclaimed!"}
            </div>
            <div className="icon">
                <PastWeekStatusIcon status={status} />
            </div>
        </div>
    );
}

function PastWeekStatusIcon({ status }: { status: number }) {
    switch (status) {
    case WeeklyGoalStatus.COMPLETED:
        return <CheckCircleIcon width="18" height="18" />;
    case WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UNKOWN:
    case WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UPKEEPER:
    case WeeklyGoalStatus.NULL:
        return <XIcon width="18" height="18" />;
    default:
        return <DonateIcon width="21" height="21" />;
    }
}

interface PastWeekDetailsModalProps {
    weekIndex: number;
    weekDetails: GetWeekDetailsResponse;
    network: Network;
    goals: WeeklyGoal["goals"];
    vowAddress: string;
    currency: Currency;
    status: WeeklyGoalStatusType;
    requiredNumberOfCompletedGoals: number;
    gymVisitsGoal: number,
    runningSessionsGoal: number,
    healthySleepNightsGoal: number,
    runningEventValidator: RunningEventValidator,
    gymVisitEventValidator: GymVisitEventValidator,
    sleepEventValidator: SleepEventValidator,
    penaltyAmount: number;
    upkeeperAddress: string;
    closeModal: () => void;
}

function PastWeekDetailsModal(props: Omit<PastWeekDetailsModalProps, 'weekDetails'>) {
    const [weekDetails, setWeekDetails] = React.useState<GetWeekDetailsResponse | null>(null);
    const status = props.status;

    React.useEffect(() => {
        getWeekDetails(props.weekIndex.toString())
            .then((r) => setWeekDetails(r));
    }, []);

    if (weekDetails == null) {
        return <div className="main"><div className="past-week-details-modal"><b>⏳ Loading Week Details...</b></div></div>;
    }

    if (status === WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UNKOWN || status === WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UPKEEPER) {
        return <PastWeekFailedDetailsModal {...props} weekDetails={weekDetails} />;
    } else if (status === WeeklyGoalStatus.NULL) {
        return <PastWeekFailedExpiredDetailsModal {...props} weekDetails={weekDetails} />;
    } else if (status === WeeklyGoalStatus.FAILED_PENDING_PENALTY) {
        return <PastWeekFailedClaimRewardDetailsModal {...props} weekDetails={weekDetails} />;
    } else {
        return <PastWeekSucceedDetailsModal {...props} weekDetails={weekDetails} />;
    }
}

function TargetGoalsList(props: {
    weekDetails: GetWeekDetailsResponse;
    gymVisitsGoal: number;
    runningSessionsGoal: number;
    healthySleepNightsGoal: number;
    gymVisitEventValidator: GymVisitEventValidator,
    runningEventValidator: RunningEventValidator,
    sleepEventValidator: SleepEventValidator,
}) {
    const { weekDetails, gymVisitsGoal, runningSessionsGoal, healthySleepNightsGoal } = props;
    const metersToKms = (distance: number) => ((distance / 1000).toFixed(1) + ' km').replaceAll('.', ',');
    const minimumDistance = metersToKms(Number(props.runningEventValidator.minimumDistanceInMeters));
    const sleepMinimumTime = formatTime(Number(props.sleepEventValidator.minimumDurationInMinutes) * 60, '');
    const gymVisitMinimumDuration = formatTime(Number(props.gymVisitEventValidator.minimumVisitTimeInMinutes) * 60, '');

    return (
        <ul className="goalsList">
            <li>
                <span>{weekDetails.goals.run2KmGoalMet ? "✔️" : "❌"}</span><b>Run ≥ {minimumDistance}</b>: Complete at least {runningSessionsGoal} running sessions, each covering at least {minimumDistance}. <small>({weekDetails.goals.runningSessions} / {runningSessionsGoal} sessions).</small>
            </li>
            <li>
                <span>{weekDetails.goals.sleptWellGoalMet ? "✔️" : "❌"}</span><b>Sleep ≥ {sleepMinimumTime}</b>: Achieve at least {healthySleepNightsGoal} nights of sleep lasting {sleepMinimumTime} or more. <small>({weekDetails.goals.healthySleepNights} / {healthySleepNightsGoal} nights).</small>
            </li>
            <li>
                <span>{weekDetails.goals.gymVisitsGoalMet ? "✔️" : "❌"}</span><b>Workout ≥ {gymVisitMinimumDuration}</b>: Complete at least {gymVisitsGoal} gym visits lasting {gymVisitMinimumDuration.replace('m', ' minutes')} or more. <small>({weekDetails.goals.gymVisits} / {gymVisitsGoal} visits).</small>
            </li>
        </ul>
    );
}

function RecordsHistory({ weekDetails, network, oracleAddress }: { weekDetails: GetWeekDetailsResponse; network: Network; oracleAddress: string }) {
    const blockExplorerLink = getAddressBlockExplorerUrl(oracleAddress, network) + '#events';

    if (weekDetails.history == null) {
        return (
            <div className="records-history">
                <h4 className="records-history__heading">🔎 Physical Activity History</h4>
                <p className="hint">
                    Records for this week are not available yet.{" "}
                    <a href={blockExplorerLink} target="_blank">Check them on Etherscan.</a>
                </p>
            </div>
        );
    }

    const rows = [
        ...weekDetails.history.runningEventProcessed.map(event => ({
            type: 'Run',
            timestamp: event.timestamp,
            detail: `${(event.distanceInMeters / 1000).toFixed(2)} km • ${formatPace(event.paceInSecondsPerKm)} • ${event.avgBpm} bpm`,
            transactionHash: event.transactionHash,
        })),
        ...weekDetails.history.gymVisitEventProcessed.map(event => ({
            type: 'Workout',
            timestamp: event.timestamp,
            detail: <> {event.durationInMinutes} min • {event.avgBpm} bpm • {event.maxBpm} bpm • ({Number(event.gymLocationLatitudeNanoDegree / 1e7)}°, {Number(event.gymLocationLongitudeNanoDegree / 1e7)}°)</>,
            transactionHash: event.transactionHash,
        })),
        ...weekDetails.history.sleepEventProcessed.map(event => ({
            type: 'Sleep',
            timestamp: event.timestamp,
            detail: `${(event.durationInMinutes / 60).toFixed(1)} hrs • ${event.avgBpm} bpm`,
            transactionHash: event.transactionHash,
        })),
    ].sort((a, b) => b.timestamp - a.timestamp);

    if (rows.length === 0) {
        return (
            <div className="records-history">
                <h4 className="records-history__heading">🔎 Physical Activity History</h4>
                <p className="hint">
                    No physical activity records reported this week.{" "}
                    <a href={blockExplorerLink} target="_blank">More details on Etherscan.</a>
                </p>
            </div>
        );
    }

    return (
        <div className="records-history">
            <h4 className="records-history__heading">🔎 Physical Activity History</h4>
            <div className="records-history__table-wrapper">
                <table className="records-table">
                    <thead>
                        <tr>
                            <th>Type</th>
                            <th>When</th>
                            <th>Details</th>
                            <th>Transaction</th>
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map((row, index) => (
                            <tr key={`${row.transactionHash}-${index}`}>
                                <td>{row.type}</td>
                                <td>{formatDate(row.timestamp, "numeric", "short")}</td>
                                <td className="record-detail">{row.detail}</td>
                                <td>
                                    <a
                                        href={getTransactionBlockExplorerUrl(row.transactionHash, network)}
                                        target="_blank"
                                        className="records-history__tx-link"
                                    >
                                        {shortAddress(row.transactionHash)}
                                    </a>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

function formatPace(secondsPerKm: number) {
    const minutes = Math.floor(secondsPerKm / 60);
    const seconds = secondsPerKm % 60;

    return `${minutes}:${seconds.toString().padStart(2, '0')} min/km`;
}


function PastWeekFailedDetailsModal({
    network,
    currency,
    weekDetails,
    ...props
}: PastWeekDetailsModalProps) {
    const penaltyDetails = weekDetails.penalty;

    const enforcerAddress = penaltyDetails != null ? shortAddress(penaltyDetails.enforcer) : 'unknown';
    const transactionUrl = penaltyDetails != null ? getTransactionBlockExplorerUrl(penaltyDetails.transactionHash, network) : '#';
    const addressUrl = penaltyDetails != null ? getAddressBlockExplorerUrl(penaltyDetails.enforcer, network) : '#';
    const vowAddressUrl = getAddressBlockExplorerUrl(props.vowAddress, network);
    const totalPenaltyAmount = formatCurrency(props.penaltyAmount, currency);
    const enforcedByUpkeeper = penaltyDetails?.enforcer === props.upkeeperAddress;
    const enforcerReward = formatCurrency(enforcedByUpkeeper ? 0 : props.penaltyAmount / 2, currency);
    const charityDonation = formatCurrency(enforcedByUpkeeper ? props.penaltyAmount : props.penaltyAmount / 2, currency);

    return (
        <div className="main">
            <div className="past-week-details-modal">
                <h4>🎯 Missed Weekly Goals</h4>
                <p>
                    This week was marked as failed because not enough goals were
                    met:
                </p>
                <TargetGoalsList
                    {...props}
                    weekDetails={weekDetails}
                    runningEventValidator={props.runningEventValidator}
                    gymVisitEventValidator={props.gymVisitEventValidator}
                    sleepEventValidator={props.sleepEventValidator}
                />
                <h4>💸 Fine Applied</h4>
                <p>
                    A fine of {totalPenaltyAmount} was deducted from the
                    contract balance and distributed as follows:
                </p>
                <ul>
                    <li>
                        {enforcerReward} →{" "}
                        <a href={addressUrl} target="_blank">
                            Vow Enforcer (<code>{enforcerAddress}</code>).
                        </a>
                    </li>
                    <li>
                        {charityDonation} →{" "}
                        <a href={GIVETH_PAGE_URL} target="_blank">
                            Giveth Charity.
                        </a>
                    </li>
                </ul>
                <h4>🔍 Fine Details</h4>
                <p>More details regarding the fine process:</p>
                <ul>
                    <li>
                        <a href={transactionUrl} target="_blank">
                            🔗 View transaction on Block Explorer.
                        </a>
                    </li>
                    <li>
                        <a href={vowAddressUrl + "#code"} target="_blank">
                            👨‍💻 Function Source Code.
                        </a>
                    </li>
                </ul>
                <RecordsHistory weekDetails={weekDetails} network={network} oracleAddress={props.vowAddress} />
            </div>
            <div className="actions">
                <button className="close-button" onClick={props.closeModal}>
                    Close
                </button>
            </div>
        </div>
    );
}

function PastWeekSucceedDetailsModal({
    weekDetails,
    network,
    closeModal,
    ...props
}: PastWeekDetailsModalProps) {
    return (
        <div className="main">
            <div className="past-week-details-modal">
                <h4>🎯 Weekly Goals Completed</h4>
                <p>
                    This week was marked as success because enough goals were
                    met:
                </p>
                <TargetGoalsList {...props} weekDetails={weekDetails} />
                <h4>🤑 No Fine Applied</h4>
                <p>
                    Since the weekly goals were completed no fine was applied
                    this week.
                </p>
                <RecordsHistory weekDetails={weekDetails} network={network} oracleAddress={props.vowAddress} />
            </div>
            <div className="actions">
                <button className="close-button" onClick={closeModal}>
                    Close
                </button>
            </div>
        </div>
    );
}

function PastWeekFailedExpiredDetailsModal(props: PastWeekDetailsModalProps) {
    return (
        <div>
            <div className="past-week-details-modal">
                <h4>Contract Expired ⚰️</h4>
                <p>
                    Weekly goals were not met but no one enforced the contract
                    and claimed the fine. No actions can be taken since the
                    contract has expired.
                </p>
            </div>
            <div className="actions">
                <button className="close-button" onClick={props.closeModal}>
                    Close
                </button>
            </div>
        </div>
    );
}

type CollectFineResponse = { transaction: string; penaltyAppliedEvent?: LogDescription, noPenaltyAppliedEvent?: LogDescription };
function PastWeekFailedClaimRewardDetailsModal({
    weekDetails,
    currency,
    penaltyAmount,
    vowAddress,
    network,
    closeModal,
    ...props
}: PastWeekDetailsModalProps) {
    const enforcerReward = formatCurrency(penaltyAmount / 2, currency);
    const enforceFuncUrl = getAddressBlockExplorerUrl(vowAddress, network) + "#writeContract#F1";
    const [isLoading, setLoading] = React.useState(false);
    const [error, setError] = React.useState<string | null>(null);
    const [response, setResponse] = React.useState<CollectFineResponse | null>(null);

    const onEnforceVowStart = () => setLoading(true);

    const onEnforceVowFail = (text: string) => {
        setLoading(false);
        setError(text);
    }

    const onEnforceVowSuccess = (transaction: string, events: LogDescription[]) => {
        setTimeout(() => {            
            setLoading(false);
            const penaltyAppliedEvent = events.find(({ name }) => name === 'PenaltyApplied');
            const noPenaltyAppliedEvent = events.find(({ name }) => name === 'NoPenaltyApplied');

            setResponse({ transaction, penaltyAppliedEvent, noPenaltyAppliedEvent });
        }, 1000);
    }

    if (error != null) {
        return (
            <div className="main">
                <div className="past-week-details-modal">
                    <h4>❌Transaction Failed!</h4>
                    <pre className="code">
                        {error}
                    </pre>
                </div>
                <div className="actions">
                    <button className="close-button" onClick={closeModal}>
                        Close
                    </button>
                </div>
            </div>
        );
    }

    if (isLoading) {
        return (
            <div className="main">
                <div className="past-week-details-modal">
                    <center>
                        <b>⏳ Sending Trasaction...</b>
                    </center>
                </div>
            </div>
        );
    }

    if (response != null && response.penaltyAppliedEvent) {
        const [weekIndex = 2, enforcerAddress = '0xabcd'] = response.penaltyAppliedEvent.args;
        const enforcerAddressLink = getAddressBlockExplorerUrl(enforcerAddress, network);

        return (
            <div className="main">
                <div className="past-week-details-modal penalty-applied">
                    <h4>Fine Collected!</h4>
                    <p>
                        You've successfully collected the fine for week #{weekIndex}.
                        The fine was split and sent to the following addresses:
                    </p>
                    <ul>
                        <li>
                            <b>{enforcerReward}</b> sent to{' '}
                            <a href={enforcerAddressLink} target="_blank">your wallet.</a>
                        </li>
                        <li>
                            <b>{enforcerReward}</b> sent to the{' '}
                            <a href={GIVETH_PAGE_URL} target="_blank">Giveth Charity.</a>
                        </li>
                    </ul>
                    <p>
                        <a href={getTransactionBlockExplorerUrl(response.transaction, network)} target="_blank">
                            🔗 Transaction: {response.transaction}
                        </a>
                    </p>
                    <p><b>Please refresh the page to see the updated week status.</b></p>
                </div>
            </div>
        );
    }

    if (response != null && response.noPenaltyAppliedEvent) {
        return (
            <div className="main">
                <div className="past-week-details-modal penalty-applied">
                    <h4>Fine Claimed by Another User!</h4>
                    <p>
                        It looks like someone got to the fine first.
                        Only the first person to claim the fine can receive it.
                        The transaction was processed, and the fine was claimed by someone else.
                    </p>
                    <p>
                        <a href={getTransactionBlockExplorerUrl(response.transaction, network)} target="_blank">
                            🔗 Transaction: {response.transaction}
                        </a>
                    </p>
                    <p><b>Please refresh the page to see the updated week status and who claimed the fine.</b></p>
                </div>
            </div>
        );
    }

    return (
        <div className="main">
            <div className="past-week-details-modal">
                <h4>🎯 Missed Weekly Goals</h4>
                <p>Not enough goals were met this week:</p>
                <TargetGoalsList {...props} weekDetails={weekDetails} />
                <h4>💰 Collect the fine</h4>
                <p>
                    Because the weekly goals were not met, a fine is now available to be collected.
                    The fine will be split as follows:
                </p>
                <ul>
                    <li>
                        <b>{enforcerReward}</b> → YOU (the enforcer).
                    </li>
                    <li>
                        <b>{enforcerReward}</b> →{' '}
                        <a href={GIVETH_PAGE_URL} target="_blank">Giveth Charity.</a>
                    </li>
                </ul>
                <h4>Option 1: Collect with a Wallet</h4>
                <p>
                    To collect the fine, connect your <b>{network}</b> wallet and confirm the transaction to call the <a href={enforceFuncUrl} target="_blank">#enforceAgreement()</a>. This option is
                    only available if you have a Wallet app installed like Metamask on your browser and will appear as an orange button on this modal.
                </p>
                <h4>Option 2: Collect Manually</h4>
                <p>
                    If you don't want or can't connect your wallet, you'll need to manually call the{' '}
                    <a href={enforceFuncUrl} target="_blank">#enforceAgreement()</a> function on the smart contract.
                </p>
                <h4>Important Notes</h4>
                <ul>
                    <li>This contract is deployed only in the <b>{network}</b> blockchain, make sure to target this network when calling <code>#enforceAgreement()</code>.</li>
                    <li>
                        Calling <code>#enforceAgreement()</code> does NOT require payment, in fact, the function is marked as non-payable and therefore any attempts to send ether with the transaction will automatically revert.
                    </li>
                    <li>
                        The <code>#enforceAgreement()</code> function is public and callable by anyone. 
                        Other users may call it and collect the fine before you resulting in no rewards. Refresh the page to see if the week is still up for enforcement.
                    </li>
                    <li>The <code>#enforceAgreement()</code> function Solidity source code is public available on Etherscan and Github.</li>
                </ul>
            </div>
            <div className="actions">
                <CallContractButton
                    network={network}
                    vowAddress={vowAddress}
                    onEnforceVowFail={onEnforceVowFail}
                    onEnforceVowStart={onEnforceVowStart}
                    onEnforceVowSuccess={onEnforceVowSuccess}
                />
                <button className="close-button" onClick={closeModal}>
                    Close
                </button>
            </div>
        </div>
    );
}
