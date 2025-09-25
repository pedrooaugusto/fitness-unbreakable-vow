import "./style.css";
import AppLogo from "../../assets/logo-icon.svg";
import { useEffect, useState } from "react";
import { convert, getContractOverview } from "./api/contract-overview";
import type { Currency, GetContractOverviewResponse } from "./types";
import ContractOverviewSection from "./components/ContractOverviewSection";
import { WithModal } from "../components/modal";
import { PastWeeksSection } from "./components/PastWeeksSection";
import CurrentWeekStatusSection from "./components/CurrentWeekStatusSection";

const MainPage = WithModal(function(props) {
    const [overview, setOverview] = useState<GetContractOverviewResponse | null>(null);
    const [currency, setCurrency] = useState<Currency>("usd");

    const { currentWeekNumber, startDate, secondsInAWeek, isContractExpired } = overview || {};

    useEffect(() => {
        getContractOverview().then(async (data) => {
            setOverview(await convert(data, currency));
        });
    }, [currency]);
    
    useEffect(() => {
        if (startDate == null || currentWeekNumber == null || secondsInAWeek == null || isContractExpired) return;

        const currentWeekStartDate = startDate + currentWeekNumber * secondsInAWeek;
        const currentWeekEndDate = (currentWeekStartDate + secondsInAWeek) * 1000;

        const timeout = currentWeekEndDate - new Date().getTime();
        const timeoutId = window.setTimeout(() => {
            getContractOverview().then(async (data) => setOverview(await convert(data, currency)));
        }, timeout);

        return () => window.clearTimeout(timeoutId);
    }, [currentWeekNumber, startDate, secondsInAWeek, isContractExpired, currency])

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
