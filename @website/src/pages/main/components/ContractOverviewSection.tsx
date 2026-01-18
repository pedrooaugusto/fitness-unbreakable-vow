import React from "react";
import type { ReactElement } from "react";
import cronParser from "cron-parser";
import CashIcon from "../../../assets/cash-icon";
import InfoIcon from "../../../assets/info-icon";
import CalendarIcon from "../../../assets/calendar-icon";
import ArticleIcon from "../../../assets/article-icon";
import LinkIcon from "../../../assets/link-icon";
import { formatCurrency, formatDate, formatTime, getAddressBlockExplorerUrl, GIVETH_PAGE_URL, timeRemaining } from "../../utils";
import { ContractPhase, WeeklyGoalStatus, type Currency, type GetContractOverviewResponse, type Network, type WeeklyGoal } from "../types";
import { SectionTitle } from "../../components/SectionTitle";
import type { WithModalProps } from "../../components/modal";
import LiveTimeCountdown from "./LiveTimeCountdown";
import FileCertificateIcon from "../../../assets/file-certificate-icon";
import QuestionMarkIcon from "../../../assets/question-mark-icon";
import { IntroModal } from "./IntroModal";

interface ContractOverviewSectionProps extends WithModalProps {
    overview: GetContractOverviewResponse;
    currency: Currency;
    changeCurrency: (currency: Currency) => void;
};

type WeekDurations = '180' | '300' | '10800' | '172800' | '259200' | '604800';
const CronInterval: Record<WeekDurations, number> = {
    '180':    1 * 60    + 180,
    '300':    3 * 60    + 300,
    '10800': 10 * 60    + 10800,
    '172800': 3 * 3600  + 172800,
    '259200': 3 * 3600  + 259200,
    '604800': 4 * 3600  + 604800,
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
    const contractBlockExplorerUrl = getAddressBlockExplorerUrl(overview.contractAddress, overview.network);

    const upkeepExecutionInterval = CronInterval[String(overview.secondsInAWeek) as unknown as WeekDurations];
    const nextUpkeeperExecTime = new Date(cronParser.parse(overview.upkeeperCronSpec, { tz: 'UTC' }).next().getTime());

    const openSecurityModalCountKey = `${overview.network}.security-modal-open-count`;
    const openSecurityModalCount = Number(localStorage.getItem(openSecurityModalCountKey) || '0');
    const isSandbox = location.pathname == '/sandbox';

    return (
        <section className="overview">
            <div className="content">
                <div
                    className={`detached-fab certified-button ${openSecurityModalCount ? '' : 'grab-attention'}`}
                    onClick={() => {
                        localStorage.setItem(openSecurityModalCountKey, (openSecurityModalCount + 1).toString());
                        openModal(
                            <SecurityModelModal
                                closeModal={closeModal}
                                publicKey={overview.publicKeyInfo}
                                network={overview.network}
                                oracleAddress={overview.oracleAddress}
                            />,
                            'Off-chain Security Model'
                        )
                    }}
                >
                    <FileCertificateIcon />
                </div>
                <SectionTitle
                    icon={<InfoIcon />}
                    text="Contract Details"
                    subtext={
                        <>
                            This page tracks the state of the <a href={contractBlockExplorerUrl} target="_blank">FitVow Smart Contract</a>, a {totalWeeks + 1}-week commitment to fitness and wellbeing, 
                            secured by locked funds, where physical activity data is verified on-chain and missed weekly goals trigger automatic fines.
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
                                network={overview.network}
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
                                network={overview.network}
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
                                network={overview.network}
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
                                network={overview.network}
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
                                network={overview.network}
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
                                network={overview.network}
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
                                network={overview.network}
                                subtext={`Week ${overview.currentWeekNumber} out of ${totalWeeks}`}
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
                                network={overview.network}
                                title="Automatic Enforcement"
                                openModal={() => 
                                    openModal(
                                        <UpkeeperInfoModal
                                            closeModal={closeModal}
                                            network={overview.network}
                                            secondsInAWeek={overview.secondsInAWeek}
                                            creationDate={overview.startDate}
                                            intervalInSeconds={upkeepExecutionInterval}
                                            upkeeperAddress={overview.upkeeperAddress}
                                            upkeeperId={overview.upkeeperId}
                                            upkeeperCronSpec={overview.upkeeperCronSpec}
                                            enforceVowFunctionUrl={enforceFunctionUrl}
                                        />,
                                        'Automatic Enforcement'
                                    )
                                }
                                value={
                                    overview.contractPhase === ContractPhase.FULLY_EXPIRED ?
                                        <>
                                            <span>EXPIRED</span>
                                        </>
                                        : nextUpkeeperExecTime == null ?
                                            <>
                                                <span>Off</span>
                                            </> :
                                            <>
                                                <LiveTimeCountdown endDate={nextUpkeeperExecTime} />
                                            </>
                                }
                            />
                        </div>
                    </div>
                </div>
                <div className="links">
                    <div
                        className={`detached-fab intro-button`}
                        onClick={() => 
                            openModal(
                                <IntroModal
                                    closeModal={closeModal}
                                    contractOverview={overview}
                                    currency={currency}
                                    isSandbox={isSandbox}
                                />,
                                'Welcome to FitVow!'
                            )
                        }
                    >
                        <QuestionMarkIcon />
                    </div>
                    <SectionTitle icon={<LinkIcon />} text="Useful Links" />
                    <div className="list">
                        <Link
                            icon={<ArticleIcon />}
                            title={<>What is this project? <small>(Article)</small></>}
                            url="#hello"
                        />
                        <Link
                            icon={<ArticleIcon />}
                            title={
                                <>
                                    {isSandbox ? 'Production' : 'Sandbox'} <small>(Environment)</small>
                                </>
                            }
                            url={`${isSandbox ? '../' : '/sandbox'}`}
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
                                    <small>(Smart Contract)</small>
                                </>
                            }
                            url={getAddressBlockExplorerUrl(overview.contractAddress, overview.network)}
                        />
                        <Link
                            icon={<ArticleIcon />}
                            title={
                                <>
                                    Physical Activity Oracle{" "}
                                    <small>(Smart Contract)</small>
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
    network: Network;
    subtext?: string;
    transaction?: "loss" | "gain";
    openModal?: () => void;
}) => {
    const statCardClickCountKey = `${props.network}.stat-card-click-count`;
    const statCardClickCount = Number(localStorage.getItem(statCardClickCountKey) || '0');

    const openModal = () => {
        if (props.openModal == null) return;

        const newCount = Number(localStorage.getItem(statCardClickCountKey) || '0') + 1;

        localStorage.setItem(statCardClickCountKey, newCount.toString());

        props.openModal?.();
    }

    return (
        <div className={`stat-info-card ${props.transaction || ""}`} onClick={openModal}>
            <p className="title">
                {props.title}
                {props.openModal && <InfoIcon color="#d9d9d9" pulsating={statCardClickCount === 0} />}
            </p>
            <p className="value">{props.value}</p>
            {props.subtext && <p className="subtext">{props.subtext}</p>}
        </div>
    )
};

const Link = (props: { icon: ReactElement; title: ReactElement; url: string; newTab?: boolean }) => (
    <a href={props.url} target={props.newTab === false ? undefined : '_blank'} rel="noopener noreferrer">
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
                    The Initial Stake is the amount of cryptocurrency (<b>native ETH</b>) locked into the contract by the Pledger at the commencement of the vow. This sum <b>({props.initialStake})</b> is held in escrow on-chain as collateral for the Fitness Unbreakable Vow.
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
                    The Funds Remaining represent the portion of the Initial Stake still held in escrow on-chain on behalf of the <b>Pledger</b> This balance reflects the Initial Stake minus any fines imposed for breaches of weekly obligations.
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
                    Whenever the contract is in breach and the <a href={props.enforceVowFunctionUrl} target="_blank">#enforceAgreement</a> function is invoked, a fine of <b>{props.penaltyAmount}</b> is imposed, half of the fine is donated directly to Giveth Charity, with the other half transferred to the enforcing party.
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
                    The Start Date marks the formal commencement of the Agreement. From this point onward, the <b>Pledger</b> is bound by the obligations set forth in the vow, secured by the Initial Stake held in escrow on-chain.
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
                    Upon expiration of the grace period, the Agreement is considered fully concluded. All remaining funds in escrow are released back to the Pledger, after deduction of any fines previously imposed. Once the grace period has elapsed, no further actions or claims may be brought under its terms.
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

function SecurityModelModal(props: KeyAttestationModalProps) {
    const ipfsLink = `https://${props.publicKey.attestation.cidFile}.ipfs.w3s.link`;
    const attestationInspector = `https://pedrooaugusto.github.io/android-key-attestation-inspector?attestationFileUrl=${ipfsLink}`;
    const appGithub = 'https://github.com/pedrooaugusto/fitness-unbreakable-vow/tree/main/%40androidapp';
    const blockExplorer = getAddressBlockExplorerUrl(props.oracleAddress, props.network);
    const CL = ({ f, l, c }: {f: string, l: string, c: string }) => <a href={blockExplorer + `#code#F${f}#L${l}`} target="_blank">{c}</a>;
    const FL = ({ f, c }: {f: string, c: string }) => <a href={blockExplorer + `#writeContract#F${f}`} target="_blank">{c}</a>;
    const FRL = ({ f, c }: {f: string, c: string }) => <a href={blockExplorer + `#readContract#F${f}`} target="_blank">{c}</a>;
    const EL = ({ l, c }: {l: string, c: string }) => <a href={l} target="_blank">{c}</a>;

    return (
        <div className="main">
            <div className="security-model-modal">
                <p>
                    <a href={appGithub} target="_blank">FitVow - Sync</a> is the mobile app responsible for collecting 
                    the pledger's physical activity data (runs, sleep, gym visits) from Android Health Connect and publishing those records to the{' '}
                    <a href={blockExplorer} target="_blank">PhysicalActivity Oracle Contract</a> which uses this data to 
                    decide whether fines should be applied. The Oracle accepts only properly signed records — any submission 
                    that fails cryptographic verification is ignored and produces no on-chain effect{' '}<CL f="1" l="91" c="[1]" />.
                </p>
                <h4>Signed submissions & on-chain checks</h4>
                <p>
                    Every record sent by FitVow-Sync is cryptographically signed with a private key whose public part is 
                    permanently registered on-chain (the Registered Key) and cannot be changed{' '}<FL f="3" c="[2]" />. The Contract verifies
                    signatures using <i>P-256 (secp256r1)</i>, ensuring that only data from the holder of the corresponding private key is accepted.
                </p>
                <h4>Device authenticity — Android Key Attestation</h4>
                <p>
                    Possession of a private key proves control but not authentic origin. To guarantee device integrity and secure key storage, 
                    FitVow-Sync uses{' '}<EL l="https://developer.android.com/privacy-and-security/security-key-attestation" c="Android Key Attestation" />.
                </p>
                <p>
                    On first install, the app generates a hardware-protected private key along with a hardware-signed Attestation Certificate 
                    issued by Google{' '}<EL l="https://github.com/pedrooaugusto/fitness-unbreakable-vow/blob/322dcb1622dd45f874ec2ca76a23812470ec3c12/%40androidapp/app/src/main/java/com/august/fitnessvowsync/security/HardwareProtectedKeyService.kt#L36-L50" c="[3]"/>.{' '}
                    This certificate confirms that the key was created and remains protected inside a verified{' '}
                    <EL l="https://source.android.com/docs/security/features/trusty" c="Trusted Execution Environment (TEE)"/>{' '}or StrongBox chip, and that it cannot be exported.
                </p>
                <p>
                    As a result, any data signed with 
                    that key is proven to originate from the FitVow-Sync app running on a genuine unrooted Android device.{' '}
                    Both the attestation certificate and public key are public available on-chain <FRL f="2" c="[4]" />.
                </p>
                <h4>App integrity — Sign-and-Forget (unique APK signing)</h4>
                <p>
                    To prevent tampering or reinstallation attacks, each FitVow-Sync APK is signed with a unique, random, 
                    ephemeral signing key — a mechanism called Sign-and-Forget. The proccess of creating such APKs happens publicly on Github Actions <EL l="https://github.com/pedrooaugusto/fitness-unbreakable-vow/actions/runs/19219023055" c="[5]" />.
                </p>
                <p>
                    Android treats apps signed with different keys as completely separate applications. A new version signed with a 
                    different key cannot be installed over the existing one — the system requires uninstalling the current app first. 
                    Uninstalling deletes all app data, including the hardware-backed private keys.
                </p>
                <p>
                    This behavior prevents a malicious actor from installing a “modified upgrade” that keeps access to the original keys. 
                    By using unique signing keys for every release, FitVow ensures that each installation has a one-to-one 
                    link to its cryptographic identity, permanently isolating it from any tampered builds.
                </p>
                <h4>Transparency & verification</h4>
                <p>
                    For transparency, the Contract stores both the Registered Key and a reference to its Attestation Certificate <FRL f="2" c="[6]" />{' '}<FRL f="3" c="[7]" />. 
                    Anyone can verify that the attested public key matches the on-chain key and that the certificate chain is signed 
                    by Google's trusted root authority. This can be done by using the 'Attestation Key Inspector' tool below or manually.
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

type UpkeeperInfoModalProps = {
    upkeeperId: string;
    upkeeperAddress: string;
    upkeeperCronSpec: string;
    enforceVowFunctionUrl: string;
    creationDate: number;
    intervalInSeconds: number;
    secondsInAWeek: number;
    network: Network;
    closeModal: () => void;
}

function UpkeeperInfoModal(props: UpkeeperInfoModalProps) {
    const upkeeperChainlink = `https://automation.chain.link/arbitrum${props.network === 'arbiSep' ? '-sepolia' : ''}/${props.upkeeperId}`;
    const approximatedCronCadence = formatTime(props.intervalInSeconds, ' and ', 'long');
    const endOfWeekBuffer = formatTime(props.intervalInSeconds - props.secondsInAWeek, ' and ', 'long');
    const blockExplorerUrl = getAddressBlockExplorerUrl(props.upkeeperAddress, props.network);

    return (
        <div className="main">
            <div className="upkeeper-info-modal">
                <p>
                    The <b>Automatic Enforcement</b> system is powered by a Chainlink Automation Upkeep
                    fully owned and configured by the <b>Fitness Unbreakable Vow</b> contract itself.
                    Once registered, the pledger cannot pause, modify, or disable it.
                </p>

                <p>
                    The Upkeeper exists primarily as a <b>final safeguard</b>. In normal circumstances, any
                    missed Weekly Goal is enforced by external callers who receive a reward for doing so.
                    However, in the unlikely event that <i>no one</i> triggers enforcement, the Upkeeper ensures
                    the Agreement cannot be bypassed by simply remaining inactive.
                </p>

                <p>
                    <b>Roughly every {approximatedCronCadence}</b>, the Upkeeper autonomously calls the{' '}
                    <a href={props.enforceVowFunctionUrl} target="_blank">#enforceAgreement</a> function. This call 
                    is intentionally scheduled to occur <b> about {endOfWeekBuffer} after each Weekly Cycle ends</b>, 
                    giving anyone ample time to manually enforce the agreement and claim the enforcement reward before automation takes over.
                </p>

                <p>
                    When enforcement is executed by the Upkeeper (i.e., before any external address calls it),
                    <b> 100% of any resulting fine is sent directly to Giveth Charity</b>. Because automation
                    does not have a “caller,” no share of the fine is distributed — the Upkeeper’s only role
                    is to uphold the Agreement in edge cases and protect the integrity of the vow.
                </p>

                <p>
                    <b>Upkeeper Address:</b>{' '}
                    <a href={blockExplorerUrl} target="_blank" rel="noopener noreferrer" style={{ wordBreak: 'break-all' }}>
                        {props.upkeeperAddress}
                    </a>
                    <br />

                    <b>Upkeeper Chainlink ID:</b>{' '}
                    <a href={upkeeperChainlink} target="_blank" rel="noopener noreferrer">
                        View on Chainlink Automation
                    </a>
                    <br />

                    <b>Upkeeper Cron Expression (UTC):</b> <span style={{ wordBreak: 'break-all' }}>{props.upkeeperCronSpec}</span>
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
