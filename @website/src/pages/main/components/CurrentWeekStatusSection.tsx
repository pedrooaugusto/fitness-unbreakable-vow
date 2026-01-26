import React, { type JSX, type ReactElement } from "react";
import {
    formatCurrency,
    formatDate,
    formatTime,
    getAddressBlockExplorerUrl,
    getTransactionBlockExplorerUrl,
    GIVETH_PAGE_URL,
    shortAddress,
} from "../../utils";
import CalendarIcon from "../../../assets/calendar-icon";
import CheckCircleIcon from "../../../assets/check-circle-icon";
import XIcon from "../../../assets/x-circle-icon";
import { SectionTitle } from "../../components/SectionTitle";
import { ContractPhase, type GetContractOverviewResponse, type GymVisitEventProcessed, type GymVisitEventValidator, type Network, type RunningEventProcessed, type RunningEventValidator, type SleepEventProcessed, type SleepEventValidator } from "../types";
import type { WithModalProps } from "../../components/modal";
import LiveTimeCountdown from "./LiveTimeCountdown";
import InfoIcon from "../../../assets/info-icon";
import { getWeekDetails } from "../api/week-details";

interface CurrentWeekStatusSectionProps extends WithModalProps {
    overview: GetContractOverviewResponse;
    currency: "usd" | "brl" | "eth";
}

export default function CurrentWeekStatusSection({
    overview,
    currency,
    openModal,
    closeModal,
}: CurrentWeekStatusSectionProps) {
    const healthySleepNights = Number(overview.currentWeekPhysicalActivityStats.sleep.count);
    const gymVisits = Number(overview.currentWeekPhysicalActivityStats.gym.count);
    const runningSessions = Number(overview.currentWeekPhysicalActivityStats.running.count);

    const totalDistanceRan = metersToKms(Number(overview.currentWeekPhysicalActivityStats.running.totalDistanceInMeters));
    const runningSessionMinimumDistance = metersToKms(Number(overview.runningValidator.minimumDistanceInMeters));

    const sleepSessionMinimumDuration = formatTime(Number(overview.sleepValidator.minimumDurationInMinutes * 60n), '');
    const totalSleptTime = formatTime(Number(overview.currentWeekPhysicalActivityStats.sleep.totalSleepInMinutes * 60n), '');

    const totalGymVisitsTime = formatTime(Number(overview.currentWeekPhysicalActivityStats.gym.totalMinutes * 60n), '');

    const currentWeekIndex = overview.allWeeks.length - 1;
    const currentWeekGoals = overview.allWeeks[currentWeekIndex].goals;

    const currentWeekGoalsMet = [
        currentWeekGoals.run2KmGoalMet,
        currentWeekGoals.gymVisitsGoalMet,
        currentWeekGoals.sleptWellGoalMet,
    ].filter(Boolean).length;

    const completedGoalsReq = overview.requiredNumberOfCompletedGoals;

    const totalPenaltyAmount = formatCurrency(overview.penaltyAmount, currency);
    const enforceFunUrl = getAddressBlockExplorerUrl(overview.contractAddress, overview.network) + "#writeContract#F2";
    const oracleEventsUrl = getAddressBlockExplorerUrl(overview.oracleAddress, overview.network) + "#events";

    return (
        <section className="current-week-results">
            <div className="content">
                <SectionTitle
                    icon={<CalendarIcon />}
                    text="Current Weekly Goals Status"
                    subtext={
                        <>
                            Each week, at least <b>{completedGoalsReq} of the 3</b> goals below must be satisfied. 
                            Failure to do so makes the Pledger liable for a <b>{totalPenaltyAmount} fine</b>, deducted from this contract and 
                            distributed between the enforcer (anyone) and the <a href={GIVETH_PAGE_URL} target="_blank">Giveth Charity</a> via the{' '}
                            <a href={enforceFunUrl} target="_blank">#enforceAgreement</a>{' '} function on the smart contract.
                        </>
                    }
                />
                <CurrentWeekInformation overview={overview} />
                <div className="weekly-goals-list">
                    <WeeklyGoal
                        met={currentWeekGoals.run2KmGoalMet}
                        mainTitle={
                            <>
                                Run for {runningSessionMinimumDistance}km{" "}
                                <small>({runningSessions}/{overview.runningSessionsGoal})</small>
                            </>
                        }
                        legend={<>total distance: {totalDistanceRan}km</>}
                        network={overview.network}
                        onClick={() =>
                            openModal(
                                <RunningSessionsGoalModal
                                    closeModal={closeModal}
                                    totalPenaltyAmount={totalPenaltyAmount}
                                    enforceVowFunctionUrl={enforceFunUrl}
                                    currentValue={runningSessions}
                                    validator={overview.runningValidator}
                                    requiredValue={overview.runningSessionsGoal}
                                    goalMet={currentWeekGoals.run2KmGoalMet}
                                    currentWeek={overview.currentWeekNumber.toString()}
                                    oracleEventsUrl={oracleEventsUrl}
                                    network={overview.network}
                                />,
                                "🏃 Running Session Goal"
                            )
                        }
                    />
                    <WeeklyGoal
                        network={overview.network}
                        met={currentWeekGoals.sleptWellGoalMet}
                        mainTitle={
                            <>
                                Sleep for {sleepSessionMinimumDuration}{" "}
                                <small>({healthySleepNights}/{overview.healthySleepNightsGoal})</small>
                            </>
                        }
                        legend={<>total slept time: {totalSleptTime}</>}
                        onClick={() =>
                            openModal(
                                <SleepGoalModal
                                    closeModal={closeModal}
                                    totalPenaltyAmount={totalPenaltyAmount}
                                    enforceVowFunctionUrl={enforceFunUrl}
                                    currentValue={healthySleepNights}
                                    requiredValue={overview.healthySleepNightsGoal}
                                    validator={overview.sleepValidator}
                                    goalMet={currentWeekGoals.sleptWellGoalMet}
                                    currentWeek={overview.currentWeekNumber.toString()}
                                    oracleEventsUrl={oracleEventsUrl}
                                    network={overview.network}
                                />,
                                `🛏️ ${sleepSessionMinimumDuration} Sleep Goal`
                            )
                        }
                    />
                    <WeeklyGoal
                        network={overview.network}
                        met={currentWeekGoals.gymVisitsGoalMet}
                        mainTitle={
                            <>
                                Gym visits <small>({gymVisits}/{overview.gymVisitsGoal})</small>
                            </>
                        }
                        legend={<>total time: {totalGymVisitsTime}</>}
                        onClick={() =>
                            openModal(
                                <GymVisitsGoalModal
                                    closeModal={closeModal}
                                    totalPenaltyAmount={totalPenaltyAmount}
                                    enforceVowFunctionUrl={enforceFunUrl}
                                    validator={overview.gymVisitValidator}
                                    currentValue={gymVisits}
                                    requiredValue={overview.gymVisitsGoal}
                                    goalMet={currentWeekGoals.gymVisitsGoalMet}
                                    currentWeek={overview.currentWeekNumber.toString()}
                                    oracleEventsUrl={oracleEventsUrl}
                                    network={overview.network}
                                />,
                                "💪 Gym Visits Goal"
                            )
                        }
                    />
                </div>
                <div className="week-status">
                    {currentWeekGoalsMet >= overview.requiredNumberOfCompletedGoals ? "Success" : "Failed"}{' • '}
                    {currentWeekGoalsMet} out of {overview.requiredNumberOfCompletedGoals} goals met
                </div>
            </div>
        </section>
    );
}

function WeeklyGoal(props: {
    met: boolean;
    mainTitle: ReactElement;
    legend: ReactElement;
    network: Network;
    onClick?: () => void;
}) {
    const clickCountKey = `${props.network}.weekly-goals-click-count`;
    const clickCount = Number(localStorage.getItem(clickCountKey) || '0');

    const onClick = () => {
        if (props.onClick == null) return;

        const newCount = Number(localStorage.getItem(clickCountKey) || '0') + 1;

        localStorage.setItem(clickCountKey, newCount.toString());

        props.onClick?.();
    }

    return (
        <div
            className={`weekly-goal ${props.met ? "met" : "not-met"}`}
            onClick={onClick}
        >
            <span><InfoIcon color="#fff" pulsating={clickCount === 0}/></span>
            {props.met ? <CheckCircleIcon /> : <XIcon />}
            <div className="main">{props.mainTitle}</div>
            <div className="legend">{props.legend}</div>
        </div>
    );
}

function CurrentWeekInformation({ overview }: { overview: GetContractOverviewResponse }) {
    const {
        currentWeekNumber,
        startDate,
        secondsInAWeek,
        contractPhase,
        requiredNumberOfCompletedGoals: goalsReq,
    } = overview;

    const currentWeekStartDate = startDate + currentWeekNumber * secondsInAWeek;
    const currentWeekEndDate = currentWeekStartDate + secondsInAWeek;

    const weekDurationInfo = (
        <>
            Week #{currentWeekNumber} • {' '}
            {formatDate(currentWeekStartDate, null)}{'  '}➡{'  '}
            {formatDate(currentWeekEndDate, null)}: {' '}
        </>
    );

    if (contractPhase === ContractPhase.FULLY_EXPIRED) {
        return (
            <div className="week-information">
                {weekDurationInfo} Since the contract has expired, no further actions can be taken.
            </div>
        );
    }

    if (contractPhase === ContractPhase.GRACE) {
        return (
            <div className="week-information">
                {weekDurationInfo} Contract is in grace period, no more activity records can be submitted.
            </div>
        );
    }
    
    const timeRemainingFormatted = <LiveTimeCountdown endDate={new Date(currentWeekEndDate * 1000)} />;

    return (
        <div className="week-information">
            {weekDurationInfo} If {goalsReq} of the weekly goals below are not met, calling #enforceAgreement in{' '}
            {timeRemainingFormatted} will result in fines.
        </div>
    );
}

type GoalModalProps = {
    closeModal: () => void;
    goalMet: boolean;
    currentValue: string | number;
    requiredValue?: string | number;
    totalPenaltyAmount: string;
    enforceVowFunctionUrl: string;
    currentWeek: string;
    oracleEventsUrl: string;
    validator: RunningEventValidator | GymVisitEventValidator | SleepEventValidator;
}

function RunningSessionsGoalModal(props: GoalModalProps & { network: Network }) {
    const validator = props.validator as RunningEventValidator;
    const requiredDistance = metersToKms(Number(validator.minimumDistanceInMeters));
    const pace = formatTime(Number(validator.maximumPaceInSecondsPerKm), '');

    return (
        <div className="main">
            <GoalDetails
                requirement={
                    <>
                        Within each seven-day period (“Weekly Term”), the Pledger shall complete{" "}
                        <strong> at least {props.requiredValue} Running Sessions.</strong>
                    </>
                }
                definition={
                    <p>
                        A valid running session has a minimum distance of <b>{requiredDistance}km</b>, pace smaller{" "}
                        than <b>{pace}/km</b>, avarage heart rate during the{" "}
                        exercise greater than <b>{Number(validator.minimumAvgBpm)}bpm</b> and cannot happen <b>during</b> a gym session.
                    </p>
                }
                verificationBulletPoints={
                    <>
                        <li>
                            FitVow - Sync integrates with the <a href="https://developer.android.com/health-and-fitness/guides/health-connect" target="_blank"><strong>Android Health Connect API</strong></a> to securely access running, sleep, heart rate and other health related metrics.
                        </li>
                        <li>
                            Health data is published to Android Health Connect by a compatible wearable device 
                            (e.g., the Pledger currently uses a <strong>Galaxy Watch 4</strong>), ensuring accurate and hardware-verified metrics. It only considers data added by the Samsumg Health app.
                        </li>
                        <li>
                            FitVow - Sync queries all running sessions in the Weekly
                            Term and submits them to the <i>PhysicalActivityOracle</i> contract that verifies if the records met the requirements.
                        </li>
                    </>
                }
                currentStatus={
                    <p>
                        {props.currentValue} out of {props.requiredValue} running sessions reported this week. <b>Weekly goal {props.goalMet ? 'met' : 'not Met'}.</b>
                    </p>
                }
                history={
                    <RunningCurrentWeekGoalHistory
                        currentWeek={props.currentWeek}
                        oracleEventsUrl={props.oracleEventsUrl}
                        network={props.network}
                    />
                }
                {...props}
            />
            <div className="actions">
                <button className="close-button" onClick={props.closeModal}>
                    Close
                </button>
            </div>
        </div>
    );
}

function SleepGoalModal(props: GoalModalProps & { network: Network }) {
    const validator = props.validator as SleepEventValidator;
    const requiredSleepDuration = formatTime(Number(validator.minimumDurationInMinutes * 60n), 'and', 'long');

    return (
        <div className="main">
            <GoalDetails
                requirement={
                    <>
                        Within each seven-day period (“Weekly Term”), the Pledger shall achieve{" "}
                        <strong>at least {props.requiredValue} separate nights of {requiredSleepDuration} or more of sleep</strong>.
                    </>
                }
                definition={
                    <p>
                        A valid sleep session has a minimum duration of <b>{requiredSleepDuration}</b> and heart rate between <b>{validator.avgBpmLowerBand}bpm</b> and <b>{validator.avgBpmUpperBand}bpm</b>.
                    </p>
                }
                verificationBulletPoints={
                    <>
                        <li>
                            FitVow - Sync integrates with the <a href="https://developer.android.com/health-and-fitness/guides/health-connect" target="_blank"><strong>Android Health Connect API</strong></a> to{' '}
                            securely access running, sleep, heart rate and other health related metrics. It only considers data added by the Samsumg Health app.
                        </li>
                        <li>
                            Health data is published to Android Health Connect by a compatible wearable device 
                            (e.g., the Pledger currently uses a <strong>Galaxy Watch 4</strong>), ensuring accurate and hardware-verified metrics.
                        </li>
                        <li>
                            FitVow - Sync queries the Pledger's sleep records for the Weekly Term and submits
                            them to the <i>PhysicalActivityOracle</i> contract that verifies if the records met the requirements.
                        </li>
                    </>
                }
                currentStatus={
                    <p>
                        {props.currentValue} out of {props.requiredValue} healthy sleep sessions reported this week. <b>Weekly goal {props.goalMet ? 'met' : 'not Met'}.</b>
                    </p>
                }
                history={
                    <SleepCurrentWeekGoalHistory
                        currentWeek={props.currentWeek}
                        oracleEventsUrl={props.oracleEventsUrl}
                        network={props.network}
                    />
                }
                
                {...props}
            />
            <div className="actions">
                <button className="close-button" onClick={props.closeModal}>
                    Close
                </button>
            </div>
        </div>
    );
}

function GymVisitsGoalModal(props: GoalModalProps & { network: Network }) {
    const validator = props.validator as GymVisitEventValidator;
    const requiredVisitDuration = formatTime(Number(validator.minimumVisitTimeInMinutes * 60n), ' ', 'long');
    const gymLocations = [validator.gymLoc1, validator.gymLoc2]
        .filter(a => a != undefined)
        .map(item => [Number(item.latitudeNanoDegree) / 1e7, Number(item.longitudeNanoDegree) / 1e7, Number(item.radiusInMeters)])
        .map(([lat, lon, radius], index, arr) => <><a href={`https://www.google.com/maps/?q=${lat},${lon}`} target="_blank"><code>({lat}°, {lon}°, {radius}m)</code></a>{index === arr.length - 1 ? '' : ', '}</>)

    return (
        <div className="main">
            <GoalDetails
                requirement={
                    <>
                        Within each seven-day period (“Weekly Term”), the Pledger shall complete{" "}
                        <strong>at least {props.requiredValue} verified gym visits</strong>.
                    </>
                }
                definition={
                    <p>
                        A valid gym visit is defined as the Pledger staying inside one of the following geofences defined by a circle: {gymLocations} and others... for a minimum of <b>{requiredVisitDuration}</b> with average and max heart rates during this period greater than <b>{validator.minimumAvgBpm}bpm</b> and <b>{validator.minimumMaxBpm}bpm</b> respectively.
                    </p>
                }
                verificationBulletPoints={
                    <>
                        <li>
                            FitVow - Sync detects gym visits using{" "} <strong>Android geofencing</strong> at registered gym locations.
                        </li>
                        <li>
                            Once a gym visit is over, it submits details regarding the visit and health data to the <i>PhysicalActivityOracle</i> contract which checks if the visit is valid.
                        </li>
                    </>
                }
                currentStatus={
                    <p>
                        {props.currentValue} out of {props.requiredValue} gym visits reported this week. <b>Weekly goal {props.goalMet ? 'met' : 'not Met'}.</b>
                    </p>
                }
                history={
                    <GymVisitCurrentWeekGoalHistory
                        currentWeek={props.currentWeek}
                        oracleEventsUrl={props.oracleEventsUrl}
                        network={props.network}
                    />
                }
                {...props}
            />
            <div className="actions">
                <button className="close-button" onClick={props.closeModal}>
                    Close
                </button>
            </div>
        </div>
    );
}


type GoalDetailsProps = {
    requirement: JSX.Element;
    verificationBulletPoints: JSX.Element;
    currentStatus: JSX.Element;
    totalPenaltyAmount: string;
    enforceVowFunctionUrl: string;
    definition: JSX.Element;
    history: JSX.Element;
};

function GoalDetails(props: GoalDetailsProps) {
    return (
        <div className="weekly-goal-modal">
            <h4>Requirement</h4>
            <p>{props.requirement}</p>
            <h4>Definition</h4>
            {props.definition}
            <h4>Current Status</h4>
            {props.currentStatus}
            <h4>Measurement and Verification</h4>
            <ul>
                <li>
                    Recorded by <a href="https://github.com/pedrooaugusto/fitness-unbreakable-vow/tree/main/%40androidapp" target="_blank">FitVow - Sync</a>, an Android
                    application installed on the Pledger's mobile device that
                    serves as the data collection agent.
                </li>
                {props.verificationBulletPoints}
                <li>
                    Upon verification, the Weekly Goal is deemed <strong>Met</strong>.
                </li>
            </ul>

            <h4>Trust and Security</h4>
            <ul>
                <li>
                    FitVow - Sync uses a{' '}<a target="_blank" href="https://source.android.com/docs/security/features/keystore/features#cryptographic_primitives"><strong>hardware-backed private key</strong></a> created at first launch.
                </li>
                <li>
                    Its <strong>public key</strong> is permanently registered
                    with the oracle and cannot be changed.
                </li>
                <li>
                    The oracle only accepts data signed by this key pair,
                    preventing falsification.
                </li>
                <li>
                    <a href="https://pedrooaugusto.github.io/blog/posts/making-missed-workouts-cost-money-with-smart-contracts/#security-model-high-level" target="_blank" rel="noopener noreferrer">
                        Learn more about anti-falsification mechanisms.
                    </a>
                </li>
            </ul>

            <h4>Legal Effect</h4>
            <ul>
                <li>
                    ✅ If met, the Weekly Status reflects compliance—no further
                    action.
                </li>
                <li>
                    ❌ If not met, a <strong>{props.totalPenaltyAmount} Fine</strong> is
                    deducted from contract funds and split equally between the
                    enforcing party and the <a href={GIVETH_PAGE_URL} target="_blank">designated charity</a>.
                </li>
                <li>
                    🤑 Any participant may enforce and collect this fine by
                    calling <a href={props.enforceVowFunctionUrl} target="_blank">#enforceAgreement</a> on the contract.
                </li>
            </ul>
            
            <h4>Current Week History</h4>
            {props.history}
        </div>
    );
}

function RunningCurrentWeekGoalHistory(props: { currentWeek: string; oracleEventsUrl: string; network: Network }) {
    const [runningSessions, setRunningSessions] = React.useState<RunningEventProcessed[] | null>(null);

    React.useEffect(() => {
        getWeekDetails(props.currentWeek.toString())
            .then((r) => setRunningSessions(r.history?.runningEventProcessed || null));
    }, []);

    const rows = [...(runningSessions) || []].sort((a, b) => b.timestamp - a.timestamp);

    if (rows.length === 0) {
        return (
            <p className="hint">
                No running sessions reported this week.{" "}
                <a href={props.oracleEventsUrl} target="_blank">More details on Etherscan.</a>
            </p>
        );
    }

    return (
        <div className="records-history">
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
                        {rows.map((event, index) => (
                            <tr key={`${event.transactionHash}-${index}`}>
                                <td>Run</td>
                                <td>{formatDate(event.timestamp, "numeric", "short")}</td>
                                <td className="record-detail">
                                    {(event.distanceInMeters / 1000).toFixed(2)} km ● {formatPace(event.paceInSecondsPerKm)} ● {event.avgBpm} bpm
                                </td>
                                <td>
                                    <a
                                        href={getTransactionBlockExplorerUrl(event.transactionHash, props.network)}
                                        target="_blank"
                                        className="records-history__tx-link"
                                    >
                                        {shortAddress(event.transactionHash)}
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

function SleepCurrentWeekGoalHistory(props: { currentWeek: string; oracleEventsUrl: string; network: Network }) {
    const [sleepSessions, setSleepSessions] = React.useState<SleepEventProcessed[] | null>(null);

    React.useEffect(() => {
        getWeekDetails(props.currentWeek.toString())
            .then((r) => setSleepSessions(r.history?.sleepEventProcessed || null));
    }, []);

    const rows = [...(sleepSessions || [])].sort((a, b) => b.timestamp - a.timestamp);

    if (rows.length === 0) {
        return (
            <p className="hint">
                No sleep sessions reported this week.{" "}
                <a href={props.oracleEventsUrl} target="_blank">More details on Etherscan.</a>
            </p>
        );
    }

    return (
        <div className="records-history">
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
                        {rows.map((event, index) => (
                            <tr key={`${event.transactionHash}-${index}`}>
                                <td>Sleep</td>
                                <td>{formatDate(event.timestamp, "numeric", "short")}</td>
                                <td className="record-detail">
                                    {(event.durationInMinutes / 60).toFixed(1)} hrs ● {event.avgBpm} bpm
                                </td>
                                <td>
                                    <a
                                        href={getTransactionBlockExplorerUrl(event.transactionHash, props.network)}
                                        target="_blank"
                                        className="records-history__tx-link"
                                    >
                                        {shortAddress(event.transactionHash)}
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

function GymVisitCurrentWeekGoalHistory(props: { currentWeek: string; oracleEventsUrl: string; network: Network }) {
    const [gymVisits, setGymVisits] = React.useState<GymVisitEventProcessed[] | null>(null);

    React.useEffect(() => {
        getWeekDetails(props.currentWeek.toString())
            .then((r) => setGymVisits(r.history?.gymVisitEventProcessed || null));
    }, []);

    const rows = [...(gymVisits || [])].sort((a, b) => b.timestamp - a.timestamp);

    if (rows.length === 0) {
        return (
            <p className="hint">
                No gym visit records reported this week.{" "}
                <a href={props.oracleEventsUrl} target="_blank">More details on Etherscan.</a>
            </p>
        );
    }

    return (
        <div className="records-history">
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
                        {rows.map((event, index) => (
                            <tr key={`${event.transactionHash}-${index}`}>
                                <td>Workout</td>
                                <td>{formatDate(event.timestamp, "numeric", "short")}</td>
                                <td className="record-detail">
                                    {event.durationInMinutes} min ● {event.avgBpm} bpm ● {event.maxBpm} bpm ● Valid Gym Location ✔
                                </td>
                                <td>
                                    <a
                                        href={getTransactionBlockExplorerUrl(event.transactionHash, props.network)}
                                        target="_blank"
                                        className="records-history__tx-link"
                                    >
                                        {shortAddress(event.transactionHash)}
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

const metersToKms = (distance: number) =>  Math.floor((distance / 1000) * 100) / 100;
