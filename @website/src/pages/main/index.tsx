import "./style.css";
import AppLogo from "../../assets/logo-icon.svg";
import { useEffect, useState, useCallback } from "react";
import { getContractOverview } from "./api/contract-overview";
import { ContractPhase, type Currency, type GetContractOverviewResponse } from "./types";
import ContractOverviewSection from "./components/ContractOverviewSection";
import { WithModal } from "../components/modal";
import { PastWeeksSection } from "./components/PastWeeksSection";
import CurrentWeekStatusSection from "./components/CurrentWeekStatusSection";
import { getWeekStardAndEndDate } from "../utils";
import { convert } from "./api/convert-currency";
import { IntroModal } from "./components/IntroModal";

const MainPage = WithModal(function(props) {
    const [overview, setOverview] = useState<GetContractOverviewResponse | null>(null);
    const [convertedOverview, setConvertedOverview] = useState<GetContractOverviewResponse | null>(null);
    const [currency, setCurrency] = useState<Currency>('usd');
    const [fetchDataError, setFetchDataError] = useState<string | null>();

    const fetchContractData = useCallback(() => {
        getContractOverview()
            .then(data => {
                setOverview(data);
                setFetchDataError(null);
            })
            .catch(err => {
                setFetchDataError((err || '').toString());
                console.error('Error fetching contract data: ', err);
            })
    }, []);

    useEffect(() => {
        fetchContractData();
    }, [fetchContractData]);

    useEffect(() => {
        if (overview == null) return;

        convert(overview, currency).then(setConvertedOverview)
    }, [overview, currency]);

    useEffect(() => {
        if (overview == null || overview.contractPhase === ContractPhase.FULLY_EXPIRED) return;
        
        const isInGracePeriod = overview.contractPhase === ContractPhase.GRACE;
        const { currentWeekNumber, startDate, secondsInAWeek, gracePeriod } = overview;

        const { weekEndDate } = getWeekStardAndEndDate(startDate, currentWeekNumber, secondsInAWeek);

        const periodEnd = isInGracePeriod ? weekEndDate + gracePeriod : weekEndDate;

        const refreshPageTimeout = periodEnd * 1000 - new Date().getTime();

        const timeoutId = window.setTimeout(fetchContractData, refreshPageTimeout + 4000);

        return () => window.clearTimeout(timeoutId);
    }, [overview, fetchContractData]);

    useEffect(() => {
        if (convertedOverview == null) return;
        
        const firstVisitKey = `${convertedOverview.network}.firstVisit`;
        const firstVisit = (localStorage.getItem(firstVisitKey) || 'true') === 'true';

        if (firstVisit) {
            localStorage.setItem(firstVisitKey, 'false');

            props.openModal(
                <IntroModal
                    closeModal={props.closeModal}
                    contractOverview={convertedOverview}
                    currency={currency}
                />,
                'Welcome to FitVow!'
            )
        }
    }, [convertedOverview]);

    console.log('ui');

    if (fetchDataError) return <Loading error={fetchDataError} />;

    if (!convertedOverview) return <Loading />;

    const version = convertedOverview.version.split('/').at(-1) as string;

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
                overview={convertedOverview}
                currency={currency}
                changeCurrency={setCurrency}
            />
            <CurrentWeekStatusSection
                openModal={props.openModal}
                closeModal={props.closeModal}
                overview={convertedOverview}
                currency={currency}
            />
            <PastWeeksSection
                openModal={props.openModal}
                closeModal={props.closeModal}
                overview={convertedOverview}
                currency={currency}
            />
            <footer>
                <span>
                    <a href={convertedOverview.version}>
                        {version.startsWith('v') ? version : version.substring(0, 7)} (Copacabana)
                    </a>
                </span>
                <span>1 por amor, 2 por <b>dinheiro ♬</b></span>
                <span><a href="https://github.com/pedrooaugusto/fitness-unbreakable-vow">Github</a></span>
            </footer>
        </div>
    );
});

export default MainPage;

function Loading(props: { error?: string }) {
    return (
        <div className="page main-page loading">
            <header className="main-header">
                <div className="logo-container">
                    <img src={AppLogo} alt="FitVow Logo" />
                    <h1>FitVow</h1>
                </div>
                <div className="tagline">Where health meets wealth — <b>and both are on the line.</b></div>
            </header>
            <section className="overview loading">
                {props.error == null && (
                    <div className="content">
                        <p>Loading contract overview...</p><br/>
                        <p><code>[[Fetching contract data from public Ethereum RPC nodes.]]</code></p>
                    </div>
                )}

                {props.error && (
                    <div className="content">
                        <p>Unable to load contract data.</p><br/>
                        <p>
                            <code>Failed to fetch contract data from public Ethereum RPC nodes: </code><br/>
                            <code>{props.error}</code>
                        </p>
                    </div>
                )}
            </section>
        </div>
    );
}
