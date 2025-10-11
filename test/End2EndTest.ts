import { time, loadFixture } from '@nomicfoundation/hardhat-toolbox/network-helpers';
import chai, { expect } from 'chai';
import hre, { network } from 'hardhat';
import chaiSubset from 'chai-subset';
import ChainlinkServer from '../scripts/chainlink-mock-server';
import { pushPhysicalActivityRecord } from './helpers/wait-utils';
import { recordEq } from './helpers/custom-chai-extensions';
import { FitnessUnbreakableVow, PhysicalActivityOracle } from '../typechain-types';
import createContractInteractions, { ContractInteraction, PushActivityRecordInteraction } from './helpers/generate-contract-interactions';
import { signPhysicalActivityRecord } from '../scripts/utils';
import { PhysicalActivityRecordStruct } from '../typechain-types/contracts/PhysicalActivityOracle';
import { WeeklyGoalStructOutput } from '../typechain-types/contracts/FitnessUnbreakableVow';
import { deployContractFixture, SEVEN_DAYS_IN_SECONDS, STAKED_AMOUNT } from './helpers/deploy-contract-fixture';

chai.use(chaiSubset);
chai.use(recordEq);

describe("EndToEndTest", function () {
    this.beforeAll(async () => {
        ChainlinkServer.start();
        // Enable auto mine so tests run faster
        await network.provider.send("evm_setAutomine", [true]);
        await network.provider.send("evm_setIntervalMining", [0]);
    });
    this.afterAll(() => ChainlinkServer.stop());

    describe("Tests that the contract is able to execute several calls and still be consistent", function () {
        it("Should process multiple random contract interactions", async function () {
            const { fitnessUnbreakableVow, physicalActivityOracle, owner, otherAccount } = await loadFixture(deployContractFixture);
            const weeklyContractInteractions = createContractInteractions(4);
            const completedGoalsHistory: WeeklyGoalStructOutput[] = [];

            for (const { weekNumber, interactions } of weeklyContractInteractions) {
                for (const interaction of interactions) {
                    switch (interaction.type) {
                        case 'PUSH_ACTIVITY_RECORD':
                            interaction.data.timestamp = await time.latest();
                            await processPushActivity(interaction, physicalActivityOracle);
                        break;
                        case 'TERMINATE_VOW':
                            await expect(fitnessUnbreakableVow.terminateVow()).to.be.revertedWith("Contract has not expired yet.");
                        break;
                        case 'ENFORCE_AGREEMENT':
                            await assertNoPenaltyWhenEnforceAgreement(fitnessUnbreakableVow);
                        break;
                    }

                    await time.increase(120);
                }

                const completedGoals = await assertCorrectPhysicalActivityRecords(weekNumber, otherAccount, interactions, fitnessUnbreakableVow, physicalActivityOracle);

                completedGoalsHistory.push(completedGoals);
            }

            await assertVowRemaningFunds(owner, otherAccount, completedGoalsHistory, fitnessUnbreakableVow);
        }).timeout(120_000);
    });
});

const INITIAL_BALANCE = hre.ethers.parseEther("10000");

async function processPushActivity(interaction: PushActivityRecordInteraction, physicalActivityOracle: PhysicalActivityOracle) {
    if (interaction.data.useWrongSignature) {
        const { signature: wrongSignature } = await signPhysicalActivityRecord({ ...interaction.data, timestamp: 32 });

        await expect(pushPhysicalActivityRecord(physicalActivityOracle, interaction.data, wrongSignature)).to.be.rejected;
    } else {
        await pushPhysicalActivityRecord(physicalActivityOracle, interaction.data);
    }
}

async function assertVowRemaningFunds(
    owner: any,
    enforcerAddress: any,
    completedGoalsHistory: WeeklyGoalStructOutput[],
    fitnessUnbreakableVow: FitnessUnbreakableVow,
) {
    const singlePenaltyAmount = await fitnessUnbreakableVow.PENALTY_AMOUNT();
    const numberOfPenaltiesApplied = completedGoalsHistory.filter(([status, ]) => status !== 1n).length;
    const totalPenaltiesAmount = singlePenaltyAmount * BigInt(numberOfPenaltiesApplied);

    const vowBalance = await hre.ethers.provider.getBalance(await fitnessUnbreakableVow.getAddress());
    const enforcerBalance = (await hre.ethers.provider.getBalance(enforcerAddress.address)) - INITIAL_BALANCE;

    expect(vowBalance).to.be.equals(STAKED_AMOUNT - totalPenaltiesAmount);
    expect(parseFloat(hre.ethers.formatEther(enforcerBalance - totalPenaltiesAmount)).toFixed(0)).to.be.equals("-0");

    // Advance seven days to make contract expire
    await time.increase(SEVEN_DAYS_IN_SECONDS);

    if (vowBalance > 0n) {
        const transaction = await fitnessUnbreakableVow.terminateVow();

        await expect(() => transaction).to.changeEtherBalance(owner, vowBalance);
        await expect(transaction).to.emit(fitnessUnbreakableVow, 'VowExpired').withArgs(vowBalance, owner.address);
    }

    await expect(fitnessUnbreakableVow.enforceAgreement()).to.be.revertedWith("Contract has expired.");
    await expect(fitnessUnbreakableVow.terminateVow()).to.be.revertedWith("No funds to release");
}

const EMPTY_RECORD = { timestamp: 0n, healthySleepNights: 0n, runDistanceMeters: 0n, gymVisits: 0n };

async function assertCorrectPhysicalActivityRecords(
    weekNumber: number,
    enforcerAddress: any,
    contractInteractions: ContractInteraction[],
    fitnessUnbreakableVow: FitnessUnbreakableVow,
    physicalActivityOracle: PhysicalActivityOracle
) {
    const highestRecord = getHighestRecord(contractInteractions);
    const completedGoals = getCompletedGoals(highestRecord);

    await assertCurrentWeekPhysicalRecordEquals(highestRecord, physicalActivityOracle);
    await assertNoPenaltyWhenEnforceAgreement(fitnessUnbreakableVow);

    await time.increase(SEVEN_DAYS_IN_SECONDS);

    await assertWhetherPenaltyShouldBeApplied(completedGoals, weekNumber, enforcerAddress, fitnessUnbreakableVow);
    await assertWeeklyGoalsCompletion(fitnessUnbreakableVow, weekNumber, completedGoals);

    return completedGoals;
}

async function assertWeeklyGoalsCompletion(fitnessUnbreakableVow: FitnessUnbreakableVow, weekNumber: number, completedGoals: WeeklyGoalStructOutput) {
    const weeklyRecors = await fitnessUnbreakableVow.getAllWeeklyGoalsRecords();
    expect(weeklyRecors[weekNumber]).to.be.deep.equals(completedGoals);
}

async function assertWhetherPenaltyShouldBeApplied(completedGoals: WeeklyGoalStructOutput, weekIndex: number, enforcerAddress: any, fitnessUnbreakableVow: FitnessUnbreakableVow) {
    const goalWasCompleted = completedGoals[0] === 1n;

    if (goalWasCompleted) {
        await assertNoPenaltyWhenEnforceAgreement(fitnessUnbreakableVow);
    } else {
        await assertPenaltyWhenEnforceAgreement(await fitnessUnbreakableVow.PENALTY_AMOUNT(), weekIndex, enforcerAddress, fitnessUnbreakableVow);
    }
}

async function assertCurrentWeekPhysicalRecordEquals(thisRecord: PhysicalActivityRecordStruct, physicalActivityOracle: PhysicalActivityOracle) {
    const [, record] = await physicalActivityOracle.getCurrentWeekPhysicalActivityRecord();
    expect(record).to.be.equalsRecord(thisRecord);   
}

async function assertNoPenaltyWhenEnforceAgreement(fitnessUnbreakableVow: FitnessUnbreakableVow) {
    await expect(fitnessUnbreakableVow.enforceAgreement()).to.emit(fitnessUnbreakableVow, 'NoPenaltyApplied');
}

async function assertPenaltyWhenEnforceAgreement(penaltyAmount: bigint, weekIndex: number, enforcerAddress: any, fitnessUnbreakableVow: FitnessUnbreakableVow) {
    const transaction = await fitnessUnbreakableVow.connect(enforcerAddress).enforceAgreement();

    await expect(() => transaction).to.changeEtherBalance(enforcerAddress, penaltyAmount);
    await expect(transaction).to.emit(fitnessUnbreakableVow, 'PenaltyApplied').withArgs(weekIndex, enforcerAddress.address);
}

function getHighestRecord(contractInteractions: ContractInteraction[]): PhysicalActivityRecordStruct {
    return contractInteractions
        .filter((interaction): interaction is PushActivityRecordInteraction => interaction.type === 'PUSH_ACTIVITY_RECORD' && !interaction.data.useWrongSignature)
        .map(interaction => interaction.data)
        .reduce((maximumRecorded: PhysicalActivityRecordStruct | null, record: PhysicalActivityRecordStruct) => {
            if (maximumRecorded === null) return { ...record };

            if (record.gymVisits > maximumRecorded.gymVisits) maximumRecorded.gymVisits = record.gymVisits;
            if (record.healthySleepNights > maximumRecorded.healthySleepNights) maximumRecorded.healthySleepNights = record.healthySleepNights;
            if (record.runDistanceMeters > maximumRecorded.runDistanceMeters) maximumRecorded.runDistanceMeters = record.runDistanceMeters;

            return maximumRecorded;
        }, null) || EMPTY_RECORD;
}

function getCompletedGoals(physicalActivityRecordStruct: PhysicalActivityRecordStruct) {
    const wentoToTheGymEnoughTimes = BigInt(physicalActivityRecordStruct.gymVisits) >= 2n;
    const ran2km = BigInt(physicalActivityRecordStruct.runDistanceMeters) > 2000n;
    const sleptWell = BigInt(physicalActivityRecordStruct.healthySleepNights) >= 2n;
    const isCompleted = (wentoToTheGymEnoughTimes && ran2km) || (wentoToTheGymEnoughTimes && sleptWell) || (ran2km && sleptWell);

    const output = [isCompleted ? 1n : 4n, wentoToTheGymEnoughTimes, ran2km, sleptWell] as WeeklyGoalStructOutput;

    return output;
}