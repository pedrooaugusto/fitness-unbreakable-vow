import "./style.css";
import AppLogo from "../../assets/logo-icon.svg";
import { useEffect, useState } from "react";
import { convert, getContractOverview } from "./api/contract-overview";
import { ContractPhase, type Currency, type GetContractOverviewResponse } from "./types";
import ContractOverviewSection from "./components/ContractOverviewSection";
import { WithModal } from "../components/modal";
import { PastWeeksSection } from "./components/PastWeeksSection";
import CurrentWeekStatusSection from "./components/CurrentWeekStatusSection";
import { getWeekStardAndEndDate } from "../utils";

const MainPage = WithModal(function(props) {
    const [overview, setOverview] = useState<GetContractOverviewResponse | null>(null);
    const [currency, setCurrency] = useState<Currency>("usd");

    const { currentWeekNumber, startDate, secondsInAWeek, contractPhase, gracePeriod } = overview || {};

    useEffect(() => {
        getContractOverview().then(async (data) => {
            setOverview(await convert(data, currency));
        });
    }, [currency]);
    
    useEffect(() => {
        if (startDate == null || currentWeekNumber == null || gracePeriod == null || secondsInAWeek == null || contractPhase === ContractPhase.FULLY_EXPIRED) return;

        const { weekEndDate: currentWeekEnd } = getWeekStardAndEndDate(startDate, currentWeekNumber, secondsInAWeek);
        const isInGracePeriod = contractPhase === ContractPhase.GRACE;

        const periodEnd = (currentWeekEnd + (isInGracePeriod ? gracePeriod : 0)) * 1000;

        const timeout = periodEnd - new Date().getTime();
        const timeoutId = window.setTimeout(() => {
            getContractOverview().then(async (data) => setOverview(await convert(data, currency)));
        }, timeout + 4000);

        return () => window.clearTimeout(timeoutId);
    }, [currentWeekNumber, startDate, secondsInAWeek, contractPhase, gracePeriod, currency])

    if (!overview) return <Loading />;

    return (
        <div className="page main-page">
            <header className="main-header">
                <div className="logo-container">
                    <img src={AppLogo} alt="FitVow Logo" />
                    <h1>FitVow</h1>
                </div>
                <div className="tagline">Where health meets wealth — <b>and both are on the line.</b></div>
            </header>
            <ContractOverviewSection
                openModal={props.openModal}
                closeModal={props.closeModal}
                overview={overview}
                currency={currency}
                changeCurrency={setCurrency}
            />
            <CurrentWeekStatusSection
                openModal={props.openModal}
                closeModal={props.closeModal}
                overview={overview}
                currency={currency}
            />
            <PastWeeksSection
                openModal={props.openModal}
                closeModal={props.closeModal}
                overview={overview}
                currency={currency}
            />
        </div>
    );
});

export default MainPage;

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
