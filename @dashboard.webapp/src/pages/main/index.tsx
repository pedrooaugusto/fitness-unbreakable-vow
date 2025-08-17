import "./style.css";
import AppLogo from "../../assets/logo-icon.svg";
import info from "../../assets/info.svg";
import { useEffect, useState, type ReactElement } from "react";
import ArticleIcon from "../../assets/article-icon";
import XIcon from "../../assets/x-circle-icon";
import CheckCircleIcon from "../../assets/check-circle-icon";
import { convert, getContractOverview } from "./contract-overview";
import type { GetContractOverviewResponse } from "./types";

export default function MainPage() {
    const [overview, setOverview] =
        useState<GetContractOverviewResponse | null>(null);
    const [currency, setCurrency] = useState<"usd" | "brl" | "eth">("brl");

    useEffect(() => {
        getContractOverview().then(async (data) => {
            setOverview(await convert(data, currency));
        });
    }, []);

    if (!overview) return <Loading />;

    const dedecutedInPeanlties = overview.initialStakedAmount - overview.currentBalance;
    const numberOfPenalties = Math.round(dedecutedInPeanlties / overview.penaltyAmount);
    const daysRemaining = timeRemaining(overview.expirationDate - overview.currentDate);
    const currentWeekNumber = overview.currentWeekNumber;
    const { highestDistanceRanInMeters, healthySleepNights, gymVisits } = overview.currentWeekMetrics;
    const highestDistanceRanInKms = Math.floor((highestDistanceRanInMeters / 1000) * 100) / 100;
    const currentWeekStartDate = overview.startDate + currentWeekNumber * overview.secondsInAWeek;
    const currentWeekEndDate = currentWeekStartDate + overview.secondsInAWeek;
    const isContractExpired = overview.isContractExpired;

    const currentWeekGoalsMet = [highestDistanceRanInKms >= 2, healthySleepNights >= 2, gymVisits >= 2].filter(Boolean).length;

    return (
        <div className="page main-page">
            <header className="main-header">
                <div className="logo-container">
                    <img src={AppLogo} alt="HealthStake Logo" />
                    <h1>HealthStake</h1>
                </div>
                <div className="tagline">Lock funds, unlock better habits.</div>
            </header>
            <section className="overview">
                <div className="content">
                    <SectionTitle
                        logo={info}
                        text="Contract Details"
                        subtext={
                            <>
                                This dashboard tracks the state of{" "}
                                <b>Fitness Unbreakable Vow</b>, a 3-month
                                commitment to physical activity backed by a
                                financial stake. Missing weekly goals results in
                                penalties, with a portion of the contract funds
                                being sent to <b>strangers and charity</b>,
                                incentivizing adherence to my fitness journey.
                            </>
                        }
                    />
                    <div className="finance-timeline">
                        <div className="finance card">
                            <SectionTitle
                                logo={info}
                                text="Financial Overview"
                            />
                            <div className="stat-info-cards">
                                <StatInfoCard
                                    title="Initial Stake"
                                    value={formatCurrency(
                                        overview.initialStakedAmount,
                                        currency
                                    )}
                                />
                                <StatInfoCard
                                    title="Funds Remaining"
                                    value={formatCurrency(
                                        overview.currentBalance,
                                        currency
                                    )}
                                    transaction="gain"
                                />
                                <StatInfoCard
                                    title="Penalties Applied"
                                    value={formatCurrency(
                                        dedecutedInPeanlties,
                                        currency
                                    )}
                                    transaction="loss"
                                    subtext={`For ${numberOfPenalties} penalties`}
                                />
                            </div>
                        </div>
                        <div className="timeline card">
                            <SectionTitle
                                logo={info}
                                text="Contract's Timeline"
                            />
                            <div className="stat-info-cards">
                                <StatInfoCard
                                    title="Start Date"
                                    value={formatDate(overview.startDate)}
                                />
                                <StatInfoCard
                                    title="End Date"
                                    value={formatDate(overview.expirationDate)}
                                />
                                <StatInfoCard
                                    title="Time Until Expiration"
                                    value={daysRemaining}
                                />
                            </div>
                        </div>
                    </div>
                    <div className="links">
                        <SectionTitle logo={info} text="Useful Links" />
                        <div className="list">
                            <Link
                                icon={<ArticleIcon />}
                                title={<>What is this project?</>}
                                url="#hello"
                            />
                            <Link
                                icon={<ArticleIcon />}
                                title={
                                    <>
                                        Source Code <small>(Github)</small>
                                    </>
                                }
                                url="https://github.com/pedrooaugusto/fitness-unbreakable-vow"
                            />
                            <Link
                                icon={<ArticleIcon />}
                                title={
                                    <>
                                        Fitness Unbreakable Vow{" "}
                                        <small>(contract)</small>
                                    </>
                                }
                                url={`https://sepolia.etherscan.io/address/${overview.contractAddress}`}
                            />
                            <Link
                                icon={<ArticleIcon />}
                                title={
                                    <>
                                        Physical Activity Oracle{" "}
                                        <small>(contract)</small>
                                    </>
                                }
                                url={`https://sepolia.etherscan.io/address/${overview.oracleAddress}`}
                            />
                            <Link
                                icon={<ArticleIcon />}
                                title={<>Giveth Charity</>}
                                url="https://giveth.io/project/Giveth-Matching-Pool-0?tab=donations"
                            />
                        </div>
                    </div>
                </div>
            </section>
            <section className="current-week-results">
                <div className="content">
                    <SectionTitle
                        logo={info}
                        text="Current Week Status"
                        subtext={
                            <>
                                If enough goals were not met, calling{" "}
                                <code>#enforceAgreement</code> at the end of
                                this week will result in a penalty. Eight
                                dollars will be deducted from the contract, four
                                will be given to the caller and four to the
                                Giveth Chararity.
                            </>
                        }
                    />
                    <CurrentWeekInformation
                        isContractExpired={isContractExpired}
                        currentWeekNumber={currentWeekNumber}
                        currentWeekStartDate={currentWeekStartDate}
                        currentWeekEndDate={currentWeekEndDate}
                        currentDate={overview.currentDate}
                    />
                    <div className="weekly-goals-list">
                        <WeeklyGoal
                            met={highestDistanceRanInKms >= 2}
                            description={
                                <>
                                    Run for 2km{" "}
                                    <small>
                                        ({highestDistanceRanInKms}/2km)
                                    </small>
                                </>
                            }
                        />
                        <WeeklyGoal
                            met={healthySleepNights >= 2}
                            description={
                                <>
                                    Slept for 8h{" "}
                                    <small>({healthySleepNights}/2)</small>
                                </>
                            }
                        />
                        <WeeklyGoal
                            met={gymVisits >= 2}
                            description={
                                <>
                                    Gym visits <small>({gymVisits}/2)</small>
                                </>
                            }
                        />
                    </div>
                    <div className="week-status">
                        Overall Status: {currentWeekGoalsMet >= 2 ? 'Success' : 'Failed'} (Goals Met: {currentWeekGoalsMet}/2)
                    </div>
                </div>
            </section>
            <section className="past-weeks-results">
                <div className="content">
                    <SectionTitle
                        logo={info}
                        text="Weekly Performance Summary"
                        subtext={
                            <>
                                This section summarizes the results of past
                                weeks, showing how many goals were met and the
                                overall status of each week.
                            </>
                        }
                    />
                    <div className="weeks-list">
                        {overview.pastWeeksGoalsResult.map((week, index) => {
                            if (!isContractExpired && index === currentWeekNumber) return null; // Skip weeks that are not over yet.

                            const numberOfGoalsMet = [week.gymVisitsGoalMet, week.sleptWellGoalMet, week.run2KmGoalMet].map(Number).reduce((a, b) => a + b, 0);
                            const overallStatus = numberOfGoalsMet >= 2;

                            return (
                                <div className={`week-card ${overallStatus ? 'met' : 'not-met'}`} key={index}>
                                    <div className="title">Week {index}</div>
                                    <div className="value">{numberOfGoalsMet}/2 Goals</div>
                                    <div className="icon">
                                        {overallStatus ? (
                                            <CheckCircleIcon />
                                        ) : (
                                            <XIcon width="18" height="18" />
                                        )}
                                    </div>
                                </div>
                            )
                        }).reverse()}
                    </div>
                </div>
            </section>
        </div>
    );
}

const SectionTitle = (props: {
    logo: string;
    text: string;
    subtext?: ReactElement;
}) => (
    <div className="title">
        <h2>
            <img src={info} alt="HealthStake Overview" />
            {props.text}
        </h2>
        <div className="subtext">{props.subtext}</div>
    </div>
);

const StatInfoCard = (props: {
    title: string;
    value: string;
    subtext?: string;
    transaction?: "loss" | "gain";
}) => (
    <div className={`stat-info-card ${props.transaction || ""}`}>
        <p className="title">{props.title}</p>
        <p className="value">{props.value}</p>
        {props.subtext && <p className="subtext">{props.subtext}</p>}
    </div>
);

const Link = (props: {
    icon: ReactElement;
    title: ReactElement;
    url: string;
}) => (
    <a href={props.url} target="_blank" rel="noopener noreferrer">
        {props.icon}
        <span>{props.title}</span>
    </a>
);

function Loading() {
    return (
        <div className="page main-page loading">
            <header className="main-header">
                <div className="logo-container">
                    <img src={AppLogo} alt="HealthStake Logo" />
                    <h1>HealthStake</h1>
                </div>
                <div className="tagline">Lock funds, unlock better habits.</div>
            </header>
            <section className="overview">
                <div className="content">
                    <p>Loading contract overview...</p>
                </div>
            </section>
        </div>
    );
}

function WeeklyGoal(props: { met: boolean; description: ReactElement }) {
    return (
        <div className={`weekly-goal ${props.met ? "met" : "not-met"}`}>
            {props.met ? <CheckCircleIcon /> : <XIcon />}
            <div className="description">{props.description}</div>
            <div className="status">{props.met ? "Met" : "Not met"}</div>
        </div>
    );
}

function formatCurrency(
    value?: number,
    currency: "usd" | "brl" | "eth" = "usd"
) {
    if (value === undefined || value === null) return "-";

    if (currency === "eth") return `${value} ETH`;

    const formatter = new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: currency === "usd" ? "USD" : "BRL",
    });

    return formatter.format(value);
}

function timeRemaining(secondsToExpire: number) {
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

function formatDate(
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
function CurrentWeekInformation(props: {
    currentWeekNumber: number;
    currentWeekStartDate: number;
    currentWeekEndDate: number;
    currentDate: number;
    isContractExpired: boolean;
}) {
    const weekDurationInfo = <>Week #{props.currentWeekNumber} goes from{" "} {formatDate(props.currentWeekStartDate, null)} to{" "} {formatDate(props.currentWeekEndDate, null)}.</>
    const timeRemainingFormatted = timeRemaining(props.currentWeekEndDate - props.currentDate)

    if (props.isContractExpired) {
        return (
            <div className="week-information">
                {weekDurationInfo} Since the contract has expired, no further actions can be taken.
            </div>
        );
    }

    return (
        <div className="week-information">
            {weekDurationInfo}{' '}
            Calling <code>#enforceAgreement</code> in {timeRemainingFormatted} will result in a penalty if goals are not met until there.
        </div>
    );
}