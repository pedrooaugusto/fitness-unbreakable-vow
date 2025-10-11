import { type JSX, type ReactElement } from "react";
import {
    formatCurrency,
    formatDate,
    getAddressBlockExplorerUrl,
    GIVETH_PAGE_URL,
} from "../../utils";
import CalendarIcon from "../../../assets/calendar-icon";
import CheckCircleIcon from "../../../assets/check-circle-icon";
import XIcon from "../../../assets/x-circle-icon";
import { SectionTitle } from "../../components/SectionTitle";
import { ContractPhase, type GetContractOverviewResponse } from "../types";
import type { WithModalProps } from "../../components/modal";
import LiveTimeCountdown from "./LiveTimeCountdown";

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
    const { highestDistanceRanInMeters, healthySleepNights, gymVisits } = overview.currentWeekMetrics;
    const highestDistanceRanInKms = metersToKms(highestDistanceRanInMeters);
    const runDistanceGoal = metersToKms(overview.runDistanceGoal);

    const currentWeekIndex = overview.pastWeeksGoalsResult.length - 1;
    const currentWeekGoals = overview.pastWeeksGoalsResult[currentWeekIndex].goals;

    const currentWeekGoalsMet = [
        currentWeekGoals.run2KmGoalMet,
        currentWeekGoals.gymVisitsGoalMet,
        currentWeekGoals.sleptWellGoalMet,
    ].filter(Boolean).length;

    const totalPenaltyAmount = formatCurrency(overview.penaltyAmount, currency);
    const enforceFunUrl = getAddressBlockExplorerUrl(overview.contractAddress, overview.network) + "#writeContract#F1";

    return (
        <section className="current-week-results">
            <div className="content">
                <SectionTitle
                    icon={<CalendarIcon />}
                    text="Current Week Status"
                    subtext={
                        <>
                            Failure to satisfy the required number (<b>{overview.requiredNumberOfCompletedGoals}</b>) of Weekly Goals
                            within the Weekly Term shall render the Pledger liable
                            for a Fine of {totalPenaltyAmount}, to be deducted from the contract
                            balance and distributed between the enforcing party (You) and the Giveth Charity. 
                            Such enforcement may be executed by invoking the{' '}
                            <a
                                style={{ color: "#f06543" }}
                                href={enforceFunUrl}
                                target="_blank"
                            >
                                <code>#enforceAgreement</code>
                            </a>
                            {' '}function of the contract.
                        </>
                    }
                />
                <CurrentWeekInformation overview={overview} />
                <div className="weekly-goals-list">
                    <WeeklyGoal
                        met={currentWeekGoals.run2KmGoalMet}
                        description={
                            <>
                                Run for {runDistanceGoal}km{" "}
                                <small>({highestDistanceRanInKms}/{runDistanceGoal}km)</small>
                            </>
                        }
                        onClick={() =>
                            openModal(
                                <RunningSessionsGoalModal
                                    closeModal={closeModal}
                                    totalPenaltyAmount={totalPenaltyAmount}
                                    enforceVowFunctionUrl={enforceFunUrl}
                                    currentValue={highestDistanceRanInKms}
                                    requiredValue={overview.runDistanceGoal}
                                    goalMet={currentWeekGoals.run2KmGoalMet}
                                />,
                                "🏃 Running Session Goal"
                            )
                        }
                    />
                    <WeeklyGoal
                        met={currentWeekGoals.sleptWellGoalMet}
                        description={
                            <>
                                Slept for 8h{" "}
                                <small>({healthySleepNights}/{overview.healthySleepNightsGoal})</small>
                            </>
                        }
                        onClick={() =>
                            openModal(
                                <SleepGoalModal
                                    closeModal={closeModal}
                                    totalPenaltyAmount={totalPenaltyAmount}
                                    enforceVowFunctionUrl={enforceFunUrl}
                                    currentValue={healthySleepNights}
                                    requiredValue={overview.healthySleepNightsGoal}
                                    goalMet={currentWeekGoals.sleptWellGoalMet}
                                />,
                                "🛏️ 8 Hours Sleep Goal"
                            )
                        }
                    />
                    <WeeklyGoal
                        met={currentWeekGoals.gymVisitsGoalMet}
                        description={
                            <>
                                Gym visits <small>({gymVisits}/{overview.gymVisitsGoal})</small>
                            </>
                        }
                        onClick={() =>
                            openModal(
                                <GymVisitsGoalModal
                                    closeModal={closeModal}
                                    totalPenaltyAmount={totalPenaltyAmount}
                                    enforceVowFunctionUrl={enforceFunUrl}
                                    currentValue={gymVisits}
                                    requiredValue={overview.gymVisitsGoal}
                                    goalMet={currentWeekGoals.gymVisitsGoalMet}
                                />,
                                "💪 Gym Visits Goal"
                            )
                        }
                    />
                </div>
                <div className="week-status">
                    Overall Status:{" "}{currentWeekGoalsMet >= overview.requiredNumberOfCompletedGoals ? "Success" : "Failed"} (Goals Met: {currentWeekGoalsMet}/{overview.requiredNumberOfCompletedGoals})
                </div>
            </div>
        </section>
    );
}

function WeeklyGoal(props: {
    met: boolean;
    description: ReactElement;
    onClick?: () => void;
}) {
    return (
        <div
            className={`weekly-goal ${props.met ? "met" : "not-met"}`}
            onClick={props.onClick}
        >
            {props.met ? <CheckCircleIcon /> : <XIcon />}
            <div className="description">{props.description}</div>
            <div className="status">{props.met ? "Met" : "Not met"}</div>
        </div>
    );
}

function CurrentWeekInformation({ overview }: { overview: GetContractOverviewResponse }) {
    const {
        currentWeekNumber,
        startDate,
        secondsInAWeek,
        contractPhase,
    } = overview;

    const currentWeekStartDate = startDate + currentWeekNumber * secondsInAWeek;
    const currentWeekEndDate = currentWeekStartDate + secondsInAWeek;

    const weekDurationInfo = (
        <>
            Week #{currentWeekNumber} goes from{" "}
            {formatDate(currentWeekStartDate, null)} to{" "}
            {formatDate(currentWeekEndDate, null)}.
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
            {weekDurationInfo} Calling <code>#enforceAgreement</code> in{" "}
            {timeRemainingFormatted} will result in a penalty if goals are not
            met until there.
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
}

function RunningSessionsGoalModal(props: GoalModalProps) {
    const goalDistance = metersToKms(Number(props.requiredValue));

    return (
        <div className="main">
            <GoalDetails
                requirement={
                    <p>
                        Within each seven-day period (“Weekly Term”), the Pledger (<em style={{ fontFamily: "cursive" }}>P.S</em>) shall complete{" "}
                        <strong> at least one (1) running session of {goalDistance} kilometers or more</strong>.
                    </p>
                }
                verificationBulletPoints={
                    <>
                        <li>
                            FitVow - Sync integrates with the <a href="https://developer.android.com/health-and-fitness/guides/health-connect" target="_blank"><strong>Android Health Connect API</strong></a> to securely access running, sleep, heart rate and other health related metrics.
                        </li>
                        <li>
                            Health data is published to Android Health Connect by a compatible wearable device 
                            (e.g., the Pledger currently uses a <strong>Galaxy Watch 4</strong>), ensuring accurate and hardware-verified metrics.
                        </li>
                        <li>
                            FitVow - Sync queries all running sessions in the Weekly
                            Term and submits the{" "}
                            <strong>longest verified distance</strong> to the
                            oracle.
                        </li>
                    </>
                }
                currentStatus={
                    <p>
                        {props.currentValue} km / {goalDistance} km: <b>{props.goalMet ? 'Met' : 'Not Met'}</b>
                    </p>
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

function SleepGoalModal(props: GoalModalProps) {
    return (
        <div className="main">
            <GoalDetails
                requirement={
                    <p>
                        Within each seven-day period (“Weekly Term”), the Pledger (<em style={{ fontFamily: "cursive" }}>P.S</em>) shall achieve{" "}
                        <strong>at least {props.requiredValue} separate nights of eight (8) or more hours of sleep</strong>.
                    </p>
                }
                verificationBulletPoints={
                    <>
                        <li>
                            FitVow - Sync integrates with the <a href="https://developer.android.com/health-and-fitness/guides/health-connect" target="_blank"><strong>Android Health Connect API</strong></a> to securely access running, sleep, heart rate and other health related metrics.
                        </li>
                        <li>
                            Health data is published to Android Health Connect by a compatible wearable device 
                            (e.g., the Pledger currently uses a <strong>Galaxy Watch 4</strong>), ensuring accurate and hardware-verified metrics.
                        </li>
                        <li>
                            FitVow - Sync queries the Pledger's sleep records for the Weekly Term,
                            counts the <strong>number of nights with ≥ 8 hours of sleep</strong>, and submits
                            that count to the oracle.
                        </li>
                    </>
                }
                currentStatus={
                    <p>
                        {props.currentValue} / {props.requiredValue} nights: <b>{props.goalMet ? 'Met' : 'Not Met'}</b>
                    </p>
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

function GymVisitsGoalModal(props: GoalModalProps) {
    return (
        <div className="main">
            <GoalDetails
                requirement={
                    <p>
                        Within each seven-day period (“Weekly Term”), the Pledger (<em style={{ fontFamily: "cursive" }}>P.S</em>) shall complete{" "}
                        <strong>at least {props.requiredValue} verified gym visits</strong>.
                    </p>
                }
                verificationBulletPoints={
                    <li>
                        FitVow - Sync detects gym visits using{" "}
                        <strong>Android geofencing</strong> at registered gym locations, applying
                        a <strong>minimum presence time</strong> requirement to confirm a valid visit,
                        and submits the total verified count to the oracle.
                    </li>
                }
                currentStatus={
                    <p>
                        {props.currentValue} / {props.requiredValue} visits: <b>{props.goalMet ? 'Met' : 'Not Met'}</b>
                    </p>
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
};

function GoalDetails(props: GoalDetailsProps) {
    return (
        <div className="weekly-goal-modal">
            <h4>Requirement</h4>
            <p>{props.requirement}</p>
            <h4>Current Status</h4>
            {props.currentStatus}
            <h4>Measurement and Verification</h4>
            <ul>
                <li>
                    Recorded by <strong>FitVow - Sync</strong>, an Android
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
                    <a href="#" target="_blank" rel="noopener noreferrer">
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
        </div>
    );
}

const metersToKms = (distance: number) =>  Math.floor((distance / 1000) * 100) / 100;