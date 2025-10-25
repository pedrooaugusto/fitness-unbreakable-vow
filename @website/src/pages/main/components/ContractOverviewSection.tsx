import React from "react";
import type { ReactElement } from "react";
import CashIcon from "../../../assets/cash-icon";
import InfoIcon from "../../../assets/info-icon";
import CalendarIcon from "../../../assets/calendar-icon";
import ArticleIcon from "../../../assets/article-icon";
import LinkIcon from "../../../assets/link-icon";
import { formatCurrency, formatDate, getAddressBlockExplorerUrl, GIVETH_PAGE_URL, timeRemaining } from "../../utils";
import { ContractPhase, WeeklyGoalStatus, type Currency, type GetContractOverviewResponse, type Network, type WeeklyGoal } from "../types";
import { SectionTitle } from "../../components/SectionTitle";
import type { WithModalProps } from "../../components/modal";
import LiveTimeCountdown from "./LiveTimeCountdown";
import FileCertificateIcon from "../../../assets/file-certificate-icon";

interface ContractOverviewSectionProps extends WithModalProps {
    overview: GetContractOverviewResponse;
    currency: Currency;
    changeCurrency: (currency: Currency) => void;
};

const penaltyWasApplied = (week: WeeklyGoal) =>
    week.status === WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UNKOWN ||
    week.status === WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UPKEEPER;

const ContractOverviewSection: React.FC<ContractOverviewSectionProps> = ({ overview, currency, changeCurrency, openModal, closeModal }) => {
    const numberOfPenalties = overview.allWeeks.filter(penaltyWasApplied).length;
    const [givenToCharity, givenToStrangers] = calculatePenalties(overview.allWeeks, overview.penaltyAmount);
    const currentBalancePercent = (overview.currentBalance / overview.initialStakedAmount - 1) * 100;
    const currentBalancePercentText = `${currentBalancePercent > 0 ? '+' : ''}${currentBalancePercent.toFixed(1)}%`;
    const totalWeeks = Math.floor((overview.expirationDate - overview.startDate) / overview.secondsInAWeek) - 1;
    const enforceFunctionUrl = getAddressBlockExplorerUrl(overview.contractAddress, overview.network) + "#writeContract#F1";

    return (
        <section className="overview">
            <div className="content">
                <div
                    className="certified"
                    onClick={() => 
                        openModal(
                            <KeyAttestationModal
                                closeModal={closeModal}
                                publicKey={overview.publicKeyInfo}
                                network={overview.network}
                                oracleAddress={overview.oracleAddress}
                            />,
                            'Android Key Attestation'
                        )
                    }
                >
                    <FileCertificateIcon />
                </div>
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

type KeyAttestationModalProps = {
    publicKey: GetContractOverviewResponse['publicKeyInfo'];
    closeModal: () => void;
    oracleAddress: string;
    network: Network;
}

function KeyAttestationModal(props: KeyAttestationModalProps) {
    const ipfsLink = `https://${props.publicKey.attestation.cidFile}.ipfs.w3s.link`;
    const attestationInspector = `https://pedrooaugusto.github.io/android-key-attestation-inspector?attestationFileUrl=${ipfsLink}`;
    const blockExplorer = getAddressBlockExplorerUrl(props.oracleAddress, props.network);
    const CK = ({ f, l, c }: {f: string, l: string, c: string }) => <a href={blockExplorer + `#code#F${f}#L${l}`} target="_blank">{c}</a>;

    return (
        <div className="main">
            <div className="weekly-goal-modal">
                <p>
                    Each Physical Activity Record submitted to the FitVow
                    Contract must be <b>cryptographically signed</b> using the private key
                    corresponding to the public key registered on-chain (the{" "}
                    <b>Registered Key</b>) <CK f="1" l="38" c="[1]" />. The Contract verifies every
                    submission using <b>P-256 (secp256r1)</b> digital signature
                    validation <CK f="5" l="61" c="[2]" />. Any record that fails verification is
                    automatically rejected and produces no on-chain effect <CK f="1" l="38" c="[3]" />.
                    <br />
                    <br />
                    Possession of the private key alone demonstrates control,
                    but not <i>authentic origin</i>. To establish device authenticity
                    and ensure the key is securely stored, FitVow employs{" "}
                    <b>Android Key Attestation</b>. When "Fit Vow - Sync", the Android app
                    that publishes data to the FitVow contract, is first installed on a device
                    a key pair is generated and the operating system
                    produces a <b>hardware-signed certificate chain</b> issued by
                    Google, confirming that the private key was created and
                    remains protected within a verified {' '}
                    <b>Trusted Execution Environment (TEE)</b> or{" "}
                    <b>StrongBox</b> chip. This attestation cannot be forged and stabilishes
                    that the private key cannot be exported, meaning that any data signed with that key 
                    originated from genuine unrooted android device.
                    <br />
                    <br />
                    For full transparency, the Contract records both the
                    Registered Key and a reference to its attestation
                    certificate <CK f="5" l="82" c="[6]" />. Anyone may independently verify that the
                    attested public key corresponds to the on-chain Registered
                    Key and that the certificate chain is signed by Google's
                    trusted root authority.
                    <br />
                    <br />
                    To review this attestation, open the{" "}
                    <b>Key Attestation Inspector</b> below and confirm that the
                    X and Y coordinates of the public key match those shown
                    here or do it manually on your own.
                </p>
                <div style={{ marginTop: 12 }}>
                    <p>
                        <b>Public Key (X):</b>{" "}<code>{props.publicKey.x || "—"}</code>
                        <br />
                        <b>Public Key (Y):</b>{" "}<code>{props.publicKey.y || "—"}</code>
                        <br />
                        <b>Attestation Challenge:</b>{" "}<code>{props.publicKey.attestation.challenge || "—"}</code>
                        <br/>
                        <b>Attestation Certificate:</b>{" "}
                        <a
                            href={ipfsLink}
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            View on IPFS
                        </a>
                    </p>
                </div>
            </div>
            <div className="actions">
                <a href={attestationInspector} target="_blank">
                    <button className="connect-to-wallet">🔍 Key Attestation Inspector</button>
                </a>
                <button className="close-button" onClick={props.closeModal}>
                    Close
                </button>
            </div>
        </div>
    );
}

function calculatePenalties(allWeeks: GetContractOverviewResponse["allWeeks"], penaltyAmount: number) {
    return allWeeks.reduce(([givenToCharity, givenToStrangers], weekDetails) => {
        if (weekDetails.status === WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UNKOWN) {
            givenToCharity += penaltyAmount * 0.5;
            givenToStrangers += penaltyAmount * 0.5;
        }

        if (weekDetails.status === WeeklyGoalStatus.FAILED_PENALTY_APPLIED_BY_UPKEEPER) {
            givenToCharity += penaltyAmount * 1;
            givenToStrangers += penaltyAmount * 0;
        }

        return [givenToCharity, givenToStrangers];
    }, [0, 0]);
}

export default ContractOverviewSection;
