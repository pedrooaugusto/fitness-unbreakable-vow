import React from "react";
import type { ReactElement } from "react";
import CashIcon from "../../../assets/cash-icon";
import InfoIcon from "../../../assets/info-icon";
import CalendarIcon from "../../../assets/calendar-icon";
import ArticleIcon from "../../../assets/article-icon";
import LinkIcon from "../../../assets/link-icon";
import { formatCurrency, formatDate, getAddressBlockExplorerUrl, GIVETH_PAGE_URL, timeRemaining } from "../../utils";
import { ContractPhase, WeeklyGoalStatus, type Currency, type GetContractOverviewResponse } from "../types";
import { SectionTitle } from "../../components/SectionTitle";
import type { WithModalProps } from "../../components/modal";
import LiveTimeCountdown from "./LiveTimeCountdown";

interface ContractOverviewSectionProps extends WithModalProps {
    overview: GetContractOverviewResponse;
    currency: Currency;
    changeCurrency: (currency: Currency) => void;
};

const ContractOverviewSection: React.FC<ContractOverviewSectionProps> = ({ overview, currency, changeCurrency, openModal, closeModal }) => {
    const numberOfPenalties = overview.pastWeeksGoalsResult.filter(week => week.status === WeeklyGoalStatus.FAILED_PENALTY_APPLIED).length;
    const [givenToCharity, givenToStrangers] = calculatePenalties(overview.pastWeeksGoalsResult);
    const currentBalancePercent = (overview.currentBalance / overview.initialStakedAmount - 1) * 100;
    const currentBalancePercentText = `${currentBalancePercent > 0 ? '+' : ''}${currentBalancePercent.toFixed(1)}%`;
    const totalWeeks = Math.floor((overview.expirationDate - overview.startDate) / overview.secondsInAWeek) - 1;
    const enforceFunctionUrl = getAddressBlockExplorerUrl(overview.contractAddress, overview.network) + "#writeContract#F1";

    return (
        <section className="overview">
            <div className="content">
                <SectionTitle
                    icon={<InfoIcon />}
                    text="Contract Details"
                    subtext={
                        <>
                            This dashboard reflects the <b>Fitness Unbreakable Vow</b>, a binding three-month commitment to 
                            well-being and good habits, secured by a financial stake. Breach of Weekly Goals 
                            results in Fines, deducted from the contract balance and distributed to third parties 
                            ensuring accountability and incentivizing the Pledger's adherence.
                        </>
                    }
                />
                <div className="finance-timeline">
                    <div className="finance card">
                        <div className="currency-dropdown-wrapper">
                            <SectionTitle
                                icon={<CashIcon />}
                                text="Financial Overview"
                            />
                            <select name="currency" value={currency} onChange={(evt) => changeCurrency(evt.target.value as Currency)}>
                                <option value="usd">USD ($)</option>
                                <option value="brl">BRL (R$)</option>
                                <option value="eth">ETH (♦)</option>
                            </select>
                        </div>
                        <div className="stat-info-cards">
                            <StatInfoCard
                                title="Initial Stake"
                                value={formatCurrency(overview.initialStakedAmount, currency)}
                                openModal={() => 
                                    openModal(
                                        <InitialStakeInfoModal
                                            closeModal={closeModal}
                                            initialStake={formatCurrency(overview.initialStakedAmount, currency)}
                                            enforceVowFunctionUrl={enforceFunctionUrl}
                                        />,
                                        'Initial Stake Info'
                                    )
                                }
                            />
                            <StatInfoCard
                                title="Funds Remaining"
                                transaction="gain"
                                value={
                                    <>
                                        {formatCurrency(overview.currentBalance, currency)}{" "}
                                        <span style={{ color: "#ff4b4e", fontSize: '12px' }}>({currentBalancePercentText})</span>
                                    </>
                                }
                                openModal={() => 
                                    openModal(
                                        <FundsRemainingInfoModal closeModal={closeModal} />,
                                        'Funds Remaining Info'
                                    )
                                }
                            />
                            <StatInfoCard
                                title="Forfeited to Enforcers"
                                value={formatCurrency(givenToStrangers, currency)}
                                transaction="loss"
                                subtext={`For ${numberOfPenalties} fines`}
                                openModal={() => 
                                    openModal(
                                        <ForfeitedInfoModal
                                            closeModal={closeModal}
                                            penaltyAmount={formatCurrency(overview.penaltyAmount, currency)}
                                            enforceVowFunctionUrl={enforceFunctionUrl}
                                        />,
                                        'Forfeited to Enforcers Info'
                                    )
                                }
                            />
                            <StatInfoCard
                                title="Donated to Charity"
                                value={formatCurrency(givenToCharity, currency)}
                                transaction="loss"
                                subtext={`For ${numberOfPenalties} fines`}
                                openModal={() => 
                                    openModal(
                                        <DonatedToCharityInfoModal
                                            closeModal={closeModal}
                                            penaltyAmount={formatCurrency(overview.penaltyAmount, currency)}
                                            enforceVowFunctionUrl={enforceFunctionUrl}
                                        />,
                                        'Donated to Charity Info'
                                    )
                                }
                            />
                        </div>
                    </div>
                    <div className="timeline card">
                        <SectionTitle
                            icon={<CalendarIcon />}
                            text="Contract's Timeline"
                        />
                        <div className="stat-info-cards">
                            <StatInfoCard
                                title="Start Date"
                                value={formatDate(overview.startDate)}
                                openModal={() => 
                                    openModal(
                                        <StartDateInfoModal closeModal={closeModal} />,
                                        'Start Date Info'
                                    )
                                }
                            />
                            <StatInfoCard
                                title="End Date"
                                value={formatDate(overview.expirationDate)}
                                openModal={() => 
                                    openModal(
                                        <EndDateInfoModal closeModal={closeModal} gracePeriod={timeRemaining(overview.gracePeriod)} />,
                                        'End Date Info'
                                    )
                                }
                            />
                            <StatInfoCard
                                title="Time Until Expiration"
                                value={
                                    overview.contractPhase === ContractPhase.GRACE ? 
                                        <>
                                            <small>grace{' '}</small>
                                            <LiveTimeCountdown endDate={new Date((overview.expirationDate + overview.gracePeriod) * 1000)} />
                                        </> :
                                        <>
                                            <LiveTimeCountdown endDate={new Date(overview.expirationDate * 1000)} />
                                        </>
                                }
                            />
                            <StatInfoCard
                                title="Week Info"
                                value={`Week #${overview.currentWeekNumber}`}
                                subtext={`Out of ${totalWeeks}`}
                            />
                        </div>
                    </div>
                </div>
                <div className="links">
                    <SectionTitle icon={<LinkIcon />} text="Useful Links" />
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
                            url={getAddressBlockExplorerUrl(overview.contractAddress, overview.network)}
                        />
                        <Link
                            icon={<ArticleIcon />}
                            title={
                                <>
                                    Physical Activity Oracle{" "}
                                    <small>(contract)</small>
                                </>
                            }
                            url={getAddressBlockExplorerUrl(overview.oracleAddress, overview.network)}
                        />
                        <Link
                            icon={<ArticleIcon />}
                            title={<>Giveth Charity</>}
                            url={GIVETH_PAGE_URL}
                        />
                    </div>
                </div>
            </div>
        </section>
    );
};

const StatInfoCard = (props: {
    title: string;
    value: string | ReactElement;
    subtext?: string;
    transaction?: "loss" | "gain";
    openModal?: () => void;
}) => (
    <div className={`stat-info-card ${props.transaction || ""}`} onClick={props.openModal}>
        <p className="title">
            {props.title}
            {props.openModal && <InfoIcon color="#a3a3a3" />}
        </p>
        <p className="value">{props.value}</p>
        {props.subtext && <p className="subtext">{props.subtext}</p>}
    </div>
);

const Link = (props: { icon: ReactElement; title: ReactElement; url: string }) => (
    <a href={props.url} target="_blank" rel="noopener noreferrer">
        {props.icon}
        <span>{props.title}</span>
    </a>
);

type InitialStakeInfoModalProps = {
    closeModal: () => void;
    initialStake: string;
    enforceVowFunctionUrl: string;
};

function InitialStakeInfoModal(props: InitialStakeInfoModalProps) {
    return (
        <div className="main">
            <div className="weekly-goal-modal">
                <p>
                    The Initial Stake is the amount of cryptocurrency locked into the contract by the <b>Pledger (<i>P.S.</i>)</b> at the commencement of the vow. This sum <b>({props.initialStake})</b> is held in escrow on-chain as collateral for the Fitness Unbreakable Vow.
                    <br /><br />
                    In the event of breach, fines may be imposed and collected by any party through invocation of the <a href={props.enforceVowFunctionUrl} target="_blank">#enforceAgreement</a> function on the smart contract. Such fines reduce the remaining balance, with forfeited amounts distributed in equal measure to the enforcing party (You) and the registered beneficiary (<a href={GIVETH_PAGE_URL} target="_blank">Giveth Charity</a>).
                    <br /><br />
                    Upon successful completion or expiration of the contract term, any unspent balance is released back to the Pledger.
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

function FundsRemainingInfoModal(props: { closeModal: () => void; }) {
    return (
        <div className="main">
            <div className="weekly-goal-modal">
                <p>
                    The Funds Remaining represent the portion of the Initial Stake still held in escrow on-chain on behalf of the <b>Pledger <i>(P.S.)</i></b> This balance reflects the Initial Stake minus any fines imposed for breaches of weekly obligations.
                    <br /><br />
                    At any point during the contract term, this amount serves as collateral, securing the Pledger's ongoing commitment.
                    <br /><br />
                    Upon expiration or successful completion of the vow, all remaining funds are released from escrow and returned directly to the Pledger.
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

type ForfeitedInfoModalProps = {
    closeModal: () => void;
    penaltyAmount: string;
    enforceVowFunctionUrl: string;
};

function ForfeitedInfoModal(props: ForfeitedInfoModalProps) {
    return (
        <div className="main">
            <div className="weekly-goal-modal">
                <p>
                    This amount reflects the share of fines awarded to enforcing parties who invoke the <a href={props.enforceVowFunctionUrl} target="_blank">#enforceAgreement</a> function when the contract is in breach.
                    <br /><br />
                    Upon breach, the first party to call <a href={props.enforceVowFunctionUrl} target="_blank">#enforceAgreement</a> imposes a fine of <b>{props.penaltyAmount}</b>. Half of this fine is transferred to the enforcing caller, and the other half is donated to the registered beneficiary (<a href={GIVETH_PAGE_URL} target="_blank">Giveth Charity</a>).
                    <br /><br />
                    If enforcement is executed by the designated <b>Upkeeper</b> — an automated process that runs weekly — the entire fine is donated to the registered beneficiary, with no portion allocated to an enforcing party.
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

function DonatedToCharityInfoModal(props: ForfeitedInfoModalProps) {
    return (
        <div className="main">
            <div className="weekly-goal-modal">
                <p>
                    This amount reflects the share of fines allocated to the registered beneficiary, <a href={GIVETH_PAGE_URL} target="_blank">Giveth Charity</a>, as a result of enforcement actions under the Agreement.
                    <br /><br />
                    Whenever the contract is in breach and the <a href={props.enforceVowFunctionUrl} target="_blank">#enforceAgreement</a> function is invoked, a fine of {props.penaltyAmount} is imposed, half of the fine is donated directly to Giveth Charity, with the other half transferred to the enforcing party.
                    <br /><br />
                    If enforcement is carried out by the designated <b>Upkeeper</b> — an automated process that executes weekly — the entire fine is donated to Giveth Charity, ensuring full beneficiary allocation in the absence of a manual enforcer.
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

function StartDateInfoModal(props: { closeModal: () => void; }) {
    return (
        <div className="main">
            <div className="weekly-goal-modal">
                <p>
                    The Start Date marks the formal commencement of the Agreement. From this point onward, the <b>Pledger <i>(P.S.)</i></b> is bound by the obligations set forth in the vow, secured by the Initial Stake held in escrow on-chain.
                    <br /><br />
                    All performance and enforcement actions are measured from the Start Date. Weekly obligations are counted forward from this date, and any breaches occurring thereafter may be subject to fines through invocation of the enforceAgreement function.                </p>
            </div>
            <div className="actions">
                <button className="close-button" onClick={props.closeModal}>
                    Close
                </button>
            </div>
        </div>
    );
}

function EndDateInfoModal(props: { closeModal: () => void; gracePeriod: string}) {
    return (
        <div className="main">
            <div className="weekly-goal-modal">
                <p>
                    The End Date marks the formal termination of the Agreement. From this point forward, no new physical activity data may be submitted. However, during the subsequent grace period of <b>{props.gracePeriod}</b>, outstanding breaches may still be enforced through the contract.
                    <br /><br />
                    Upon expiration of the grace period, the Agreement is considered fully concluded. All remaining funds in escrow are released back to the Pledger (P.S.), after deduction of any fines previously imposed. Once the grace period has elapsed, no further actions or claims may be brought under its terms.
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

function calculatePenalties(pastWeeksGoalsResult: GetContractOverviewResponse["pastWeeksGoalsResult"]) {
    return pastWeeksGoalsResult.reduce(([givenToCharity, givenToStrangers], { penaltyDetails }) => {
        if (penaltyDetails) {
            givenToCharity += penaltyDetails.amount * (penaltyDetails.enforcedByUpkeeper ? 1 : 0.5);
            givenToStrangers += penaltyDetails.amount * (penaltyDetails.enforcedByUpkeeper ? 0 : 0.5);
        }
        return [givenToCharity, givenToStrangers];
    }, [0, 0]);
}

export default ContractOverviewSection;
