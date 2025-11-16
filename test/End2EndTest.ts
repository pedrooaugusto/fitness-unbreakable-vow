import { time, loadFixture } from '@nomicfoundation/hardhat-toolbox/network-helpers';
import chai, { expect } from 'chai';
import hre, { network } from 'hardhat';
import chaiSubset from 'chai-subset';
import { PhysicalActivityStatsStruct } from '../typechain-types/contracts/PhysicalActivityOracle';
import { WeeklyGoalStructOutput } from '../typechain-types/contracts/FitnessUnbreakableVow';
import { FitnessUnbreakableVow, PhysicalActivityOracle } from '../typechain-types';
import { recordEq } from './helpers/custom-chai-extensions';
import createContractInteractions, { ContractInteraction, numberOfFines, PublishEventInteraction } from './helpers/generate-contract-interactions';
import { deployContractFixture, NUMBER_OF_CYLES, SEVEN_DAYS_IN_SECONDS, STAKED_AMOUNT } from './helpers/deploy-contract-fixture';
import { EMPTY_GYM_VISIT_STAT, EMPTY_RUNNING_STAT, EMPTY_SLEEP_STAT, mergeGymVisit, mergeRunning, mergeSleep } from './helpers/Stats';

chai.use(chaiSubset);
chai.use(recordEq);

describe("EndToEndTest", function () {
    this.beforeAll(async () => {
        // Enable auto mine so tests run faster
        await network.provider.send("evm_setAutomine", [true]);
        await network.provider.send("evm_setIntervalMining", [0]);
    });

    describe("Tests that the contract is able to execute several calls and still be consistent", function () {
        it("Should process multiple random contract interactions", async function () {
            const { fitnessUnbreakableVow, physicalActivityOracle, owner, otherAccount } = await loadFixture(deployContractFixture);

            const weeklyContractInteractions = createContractInteractions(Math.floor(NUMBER_OF_CYLES));
            const completedGoalsHistory: WeeklyGoalStructOutput[] = [];

            for (const { weekNumber, interactions } of weeklyContractInteractions) {
                for (const interaction of interactions) {
                    switch (interaction.type) {
                        case 'PUBLISH_ACTIVITY_EVENT':
                            await processPublishEvent(interaction, physicalActivityOracle);
                        break;

                        case 'TERMINATE_VOW':
                            await expect(fitnessUnbreakableVow.terminateVow()).to.be.revertedWith("Contract has not expired yet.");
                        break;

                        case 'ENFORCE_AGREEMENT':
                            await assertNoPenaltyWhenEnforceAgreement(fitnessUnbreakableVow);
                        break;
                    }

                    //console.log(interaction.data?.request);

                    await time.increase(20);
                }

                const completedGoals = await assertEndOfWeek(weekNumber, otherAccount, interactions, fitnessUnbreakableVow, physicalActivityOracle);

                completedGoalsHistory.push(completedGoals);
            }

            await assertRemaningFunds(owner, otherAccount, completedGoalsHistory, fitnessUnbreakableVow);
            assertAllWeeksStatusMatch(completedGoalsHistory, await fitnessUnbreakableVow.getAllWeeklyGoalsRecords());

            console.log(await physicalActivityOracle.listAllPhysicalActivityStats());

        }).timeout(120_000);
    });
});

async function processPublishEvent(interaction: PublishEventInteraction, physicalActivityOracle: PhysicalActivityOracle) {
    await interaction.data.create(await time.latest());

    const publishEvent = async() => {
        const response = await physicalActivityOracle.publishPhysicalActivityEvent(interaction.data.request!);
        const transaction = await response.wait();
    }

    if (interaction.data.request!.useWrongSignature) {
        await expect(publishEvent()).to.be.rejected;
    } else {
        await publishEvent();
    }
}

async function assertRemaningFunds(
    owner: any,
    enforcerAddress: any,
    completedGoalsHistory: WeeklyGoalStructOutput[],
    fitnessUnbreakableVow: FitnessUnbreakableVow,
) {
    const INITIAL_BALANCE = hre.ethers.parseEther("10000");
    const abs = (n: bigint) => (n < 0n) ? -n : n;
    const totalFines = await fitnessUnbreakableVow.PENALTY_AMOUNT() * BigInt(numberOfFines(completedGoalsHistory));
    const vowBalance = await hre.ethers.provider.getBalance(await fitnessUnbreakableVow.getAddress());
    const enforcerBalance = abs(INITIAL_BALANCE - await hre.ethers.provider.getBalance(enforcerAddress.address));
    const enforcerFines = totalFines / 2n;
    const diff = enforcerBalance - enforcerFines;

    expect(vowBalance).to.be.equals(STAKED_AMOUNT - totalFines);
    expect(parseFloat(hre.ethers.formatEther(diff))).to.be.lessThan(0.0009);

    // Advance seven days to make contract expire
    await time.increase(SEVEN_DAYS_IN_SECONDS);

    if (vowBalance > 0n) {
        const transaction = await fitnessUnbreakableVow.terminateVow();

        await expect(() => transaction).to.changeEtherBalance(owner, vowBalance);
        await expect(transaction).to.emit(fitnessUnbreakableVow, 'VowTeminated').withArgs(vowBalance, owner.address);
    }

    await expect(fitnessUnbreakableVow.enforceAgreement()).to.be.revertedWith("Contract has expired.");
    await expect(fitnessUnbreakableVow.terminateVow()).to.be.revertedWith("No funds to release");
}

export function assertAllWeeksStatusMatch(expectedWeeklyGoalsHistory: WeeklyGoalStructOutput[], actualWeeklyGoalsHistory: WeeklyGoalStructOutput[]) {
    for (let weekIndex = 0; weekIndex < actualWeeklyGoalsHistory.length; weekIndex++) {
        expect(Number(actualWeeklyGoalsHistory[weekIndex][0])).to.be.equals(Number(expectedWeeklyGoalsHistory[weekIndex][0]));
    }
}

async function assertEndOfWeek(
    weekNumber: number,
    enforcerAddress: any,
    contractInteractions: ContractInteraction[],
    fitnessUnbreakableVow: FitnessUnbreakableVow,
    physicalActivityOracle: PhysicalActivityOracle
) {
    const finalWeekStats = getFinalStats(contractInteractions);
    const weeklyGoalsStatus = getWeeklyGoalsStatus(finalWeekStats);

    expect((await physicalActivityOracle.getCurrentWeekPhysicalActivityStats())[1]).to.be.equalsStats(finalWeekStats);

    await assertNoPenaltyWhenEnforceAgreement(fitnessUnbreakableVow);

    await time.increase(SEVEN_DAYS_IN_SECONDS);

    await assertWhetherPenaltyShouldBeApplied(weeklyGoalsStatus, weekNumber, enforcerAddress, fitnessUnbreakableVow);
    await assertWeeklyGoalsCompletion(fitnessUnbreakableVow, weekNumber, weeklyGoalsStatus);

    return weeklyGoalsStatus;
}

async function assertWeeklyGoalsCompletion(fitnessUnbreakableVow: FitnessUnbreakableVow, weekNumber: number, completedGoals: WeeklyGoalStructOutput) {
    const weeklyRecors = await fitnessUnbreakableVow.getAllWeeklyGoalsRecords();

    expect(weeklyRecors[weekNumber][0]).to.be.equals(completedGoals[0]);
    expect(weeklyRecors[weekNumber][1]).to.be.equals(completedGoals[1]);
    expect(weeklyRecors[weekNumber][2]).to.be.equals(completedGoals[2]);
    expect(weeklyRecors[weekNumber][3]).to.be.equals(completedGoals[3]);
}

async function assertWhetherPenaltyShouldBeApplied(completedGoals: WeeklyGoalStructOutput, weekIndex: number, enforcerAddress: any, fitnessUnbreakableVow: FitnessUnbreakableVow) {
    const goalWasCompleted = completedGoals[0] === 1n;

    if (goalWasCompleted) {
        await assertNoPenaltyWhenEnforceAgreement(fitnessUnbreakableVow);
    } else {
        await assertPenaltyWhenEnforceAgreement(await fitnessUnbreakableVow.PENALTY_AMOUNT(), weekIndex, enforcerAddress, fitnessUnbreakableVow);
    }
}

async function assertNoPenaltyWhenEnforceAgreement(fitnessUnbreakableVow: FitnessUnbreakableVow) {
    await expect(fitnessUnbreakableVow.enforceAgreement()).to.emit(fitnessUnbreakableVow, 'NoPenaltyApplied');
}

async function assertPenaltyWhenEnforceAgreement(penaltyAmount: bigint, weekIndex: number, enforcerAddress: any, fitnessUnbreakableVow: FitnessUnbreakableVow) {
    const weekStauts = Number((await fitnessUnbreakableVow.getAllWeeklyGoalsRecords())[weekIndex][0]);

    expect(weekStauts).to.be.equals(3);

    const transaction = await fitnessUnbreakableVow.connect(enforcerAddress).enforceAgreement();

    await expect(() => transaction).to.changeEtherBalance(enforcerAddress, penaltyAmount / 2n);
    await expect(transaction).to.emit(fitnessUnbreakableVow, 'PenaltyApplied').withArgs(weekIndex, enforcerAddress.address);
}

function getFinalStats(contractInteractions: ContractInteraction[]): PhysicalActivityStatsStruct {
    const validPublishInteractions = contractInteractions
        .filter(interaction => interaction.type === 'PUBLISH_ACTIVITY_EVENT')
        .filter(interaction => !interaction.data.request!.useWrongSignature);

    const finalStats = { timestamp: 0, sleep: { ...EMPTY_SLEEP_STAT }, running: { ...EMPTY_RUNNING_STAT }, gym: { ...EMPTY_GYM_VISIT_STAT } }; 

    for (const { data: { request } } of validPublishInteractions) {
        finalStats.running = mergeRunning(finalStats.running, request!.running || []);
        finalStats.sleep = mergeSleep(finalStats.sleep, request!.sleep || []);
        finalStats.gym = mergeGymVisit(finalStats.gym, request!.gymVisit || []);
    }

    return finalStats;
}

function getWeeklyGoalsStatus(stats: PhysicalActivityStatsStruct) {
    const wentoToTheGymEnoughTimes = BigInt(stats.gym.count) >= 2n;
    const ran2km = BigInt(stats.running.count) >= 2n;
    const sleptWell = BigInt(stats.sleep.count) >= 2n;

    const isCompleted = (wentoToTheGymEnoughTimes && ran2km) || (wentoToTheGymEnoughTimes && sleptWell) || (ran2km && sleptWell);

    const output = [isCompleted ? 1n : 5n, wentoToTheGymEnoughTimes, ran2km, sleptWell, isCompleted ? 0n : 1n] as WeeklyGoalStructOutput;

    return output;
}