import { type JSX, type ReactElement } from "react";
import {
    formatCurrency,
    formatDate,
    formatTime,
    getAddressBlockExplorerUrl,
    GIVETH_PAGE_URL,
} from "../../utils";
import CalendarIcon from "../../../assets/calendar-icon";
import CheckCircleIcon from "../../../assets/check-circle-icon";
import XIcon from "../../../assets/x-circle-icon";
import { SectionTitle } from "../../components/SectionTitle";
import { ContractPhase, type GetContractOverviewResponse, type GymVisitEventValidator, type RunningEventValidator, type SleepEventValidator } from "../types";
import type { WithModalProps } from "../../components/modal";
import LiveTimeCountdown from "./LiveTimeCountdown";
import InfoIcon from "../../../assets/info-icon";

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
                        mainTitle={
                            <>
                                Jog for {runningSessionMinimumDistance}km{" "}
                                <small>({runningSessions}/{overview.runningSessionsGoal})</small>
                            </>
                        }
                        legend={<>total distance: {totalDistanceRan}km</>}
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
                                />,
                                "🏃 Running Session Goal"
                            )
                        }
                    />
                    <WeeklyGoal
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
                                />,
                                `🛏️ ${sleepSessionMinimumDuration} Sleep Goal`
                            )
                        }
                    />
                    <WeeklyGoal
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
    mainTitle: ReactElement;
    legend: ReactElement;
    onClick?: () => void;
}) {
    return (
        <div
            className={`weekly-goal ${props.met ? "met" : "not-met"}`}
            onClick={props.onClick}
        >
            <span><InfoIcon color="#fff" /></span>
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
    validator: RunningEventValidator | GymVisitEventValidator | SleepEventValidator;
}

function RunningSessionsGoalModal(props: GoalModalProps) {
    const validator = props.validator as RunningEventValidator;
    const requiredDistance = metersToKms(Number(validator.minimumDistanceInMeters));

    return (
        <div className="main">
            <GoalDetails
                requirement={
                    <>
                        Within each seven-day period (“Weekly Term”), the Pledger (<em style={{ fontFamily: "cursive" }}>P.S</em>) shall complete{" "}
                        <strong> at least {props.requiredValue} Running Sessions.</strong>
                    </>
                }
                definition={
                    <p>
                        A valid running session has a minimum distance of <b>{requiredDistance}km</b>, pace smaller{" "}
                        than <b>{Number(validator.maximumPaceInSecondsPerKm / 60n)}min/km</b> and avarage heart rate during the{" "}
                        exercise greater than <b>{Number(validator.minimumAvgBpm)}bpm</b>.
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
                        {props.currentValue} / {props.requiredValue}: <b>{props.goalMet ? 'Met' : 'Not Met'}</b>
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
    const validator = props.validator as SleepEventValidator;
    const requiredSleepDuration = formatTime(Number(validator.minimumDurationInMinutes * 60n));

    return (
        <div className="main">
            <GoalDetails
                requirement={
                    <>
                        Within each seven-day period (“Weekly Term”), the Pledger (<em style={{ fontFamily: "cursive" }}>P.S</em>) shall achieve{" "}
                        <strong>at least {props.requiredValue} separate nights of {requiredSleepDuration} or more hours of sleep</strong>.
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
    const validator = props.validator as GymVisitEventValidator;
    const requiredVisitDuration = formatTime(Number(validator.minimumVisitTimeInMinutes * 60n));
    const gymLocations = [validator.gym1Location, validator.gym2Location, validator.gym3Location]
        .map(item => [Number(item.latitudeNanoDegree) / 1e7, Number(item.longitudeNanoDegree) / 1e7])
        .map(([lat, lon], index, arr) => <><a href={`https://www.google.com/maps/?q=${lat},${lon}`} target="_blank"><code>({lat}°, {lon}°)</code></a>{index === arr.length - 1 ? '' : ' or '}</>)

    return (
        <div className="main">
            <GoalDetails
                requirement={
                    <>
                        Within each seven-day period (“Weekly Term”), the Pledger (<em style={{ fontFamily: "cursive" }}>P.S</em>) shall complete{" "}
                        <strong>at least {props.requiredValue} verified gym visits</strong>.
                    </>
                }
                definition={
                    <p>
                        A valid gym visit ocurs up to X meters of either gym locations {gymLocations}, has a minimum duration of <b>{requiredVisitDuration}</b> and average heart rate during the visit greater than <b>{validator.minimumAvgBpm}bpm</b>.
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
    definition: JSX.Element;
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