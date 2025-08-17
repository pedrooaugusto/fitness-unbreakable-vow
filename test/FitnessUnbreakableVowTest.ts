import { time, loadFixture } from '@nomicfoundation/hardhat-toolbox/network-helpers';
import chai, { expect } from 'chai';
import chaiSubset from 'chai-subset';
import ChainlinkServer from '../scripts/chainlink-mock-server';
import { pushPhysicalActivityRecord } from './helpers/wait-utils';
import { recordEq } from './helpers/custom-chai-extensions';
import { calculatePenaltyAmount, deployContractFixture, SEVEN_DAYS_IN_SECONDS, STAKED_AMOUNT } from './helpers/deploy-contract-fixture';

chai.use(chaiSubset);
chai.use(recordEq);

describe("FitnessUnbreakableVow", function () {
    this.beforeAll(() => ChainlinkServer.start());
    this.afterAll(() => ChainlinkServer.stop());

    describe("Test Penalty Amounts and Staked Values", function () {
        it("Should have the total inital staked amount set", async function () {
            const { fitnessUnbreakableVow: contract  } = await loadFixture(deployContractFixture);

            expect(await contract.STAKED_AMOUNT()).to.be.eq(STAKED_AMOUNT);
        });

        it("Should have the penalty amount set", async function () {
            const { fitnessUnbreakableVow: contract } = await loadFixture(deployContractFixture);
            const expectedPenaltyAmount = await calculatePenaltyAmount(contract);

            expect(await contract.PENALTY_AMOUNT()).to.be.eq(expectedPenaltyAmount);
        });
    });

    describe("EnforceAgreenent tests", async function () {
        const recordToAdd1 = { timestamp: 0, runDistanceMeters: 42, healthySleepNights: 0, gymVisits: 0 };
        const recordToAdd2 = { timestamp: 0, runDistanceMeters: 2352, healthySleepNights: 1, gymVisits: 2 };

        it("Should not penalize in the first week", async function () {
            const { fitnessUnbreakableVow: contract } = await loadFixture(deployContractFixture);

            await expect(contract.enforceAgreement()).to.emit(contract, 'NoPenaltyApplied');
        });

        it("Should penalize after the first week and no activity record was submited", async function () {
            const { fitnessUnbreakableVow: contract, owner } = await loadFixture(deployContractFixture);
            const penaltyAmount = await calculatePenaltyAmount(contract);
            await time.increase(SEVEN_DAYS_IN_SECONDS);

            const transaction = await contract.enforceAgreement();

            await expect(() => transaction).to.changeEtherBalance(owner, penaltyAmount);
            await expect(transaction).to.emit(contract, 'PenaltyApplied').withArgs(penaltyAmount, owner.address);
        });

        it("Should penalize after the first week and a insuficient activity record was submited", async function () {
            const { fitnessUnbreakableVow: contract, physicalActivityOracle, owner } = await loadFixture(deployContractFixture);
            const penaltyAmount = await calculatePenaltyAmount(contract);
            
            await pushPhysicalActivityRecord(physicalActivityOracle, recordToAdd1);
            await time.increase(SEVEN_DAYS_IN_SECONDS);

            const transaction = await contract.enforceAgreement();

            await expect(() => transaction).to.changeEtherBalance(owner, penaltyAmount);
            await expect(transaction).to.emit(contract, 'PenaltyApplied').withArgs(penaltyAmount, owner.address);
        });

        it("Should sent penalty value to charity when called by upkeep", async function () {
            const { fitnessUnbreakableVow: contract, physicalActivityOracle, owner, otherAccount2 } = await loadFixture(deployContractFixture);
            const penaltyAmount = await calculatePenaltyAmount(contract);
            
            await pushPhysicalActivityRecord(physicalActivityOracle, recordToAdd1);
            await time.increase(SEVEN_DAYS_IN_SECONDS);

            const contractWithOtherAccount = await contract.connect(otherAccount2);
            const transaction = await contractWithOtherAccount.enforceAgreement();

            await expect(() => transaction).to.changeEtherBalance("0x6e8873085530406995170Da467010565968C7C62", penaltyAmount);
            await expect(transaction).to.emit(contractWithOtherAccount, 'PenaltyApplied').withArgs(penaltyAmount, "0x6e8873085530406995170Da467010565968C7C62");
        });

        it("Should penalize multiple weeks in a row if no suficient activity was submited", async function () {
            const { fitnessUnbreakableVow: contract, physicalActivityOracle, owner } = await loadFixture(deployContractFixture);
            const penaltyAmount = await calculatePenaltyAmount(contract);

            // first week failed
            await pushPhysicalActivityRecord(physicalActivityOracle, recordToAdd1);
            await time.increase(SEVEN_DAYS_IN_SECONDS);

            // second week failed
            await pushPhysicalActivityRecord(physicalActivityOracle, { ...recordToAdd1, healthySleepNights: 1 });
            await time.increase(SEVEN_DAYS_IN_SECONDS);

            // third week sucessed
            await pushPhysicalActivityRecord(physicalActivityOracle, recordToAdd2);

            // Penalty applied for failing in the first week
            expect(await contract.isPenaltyLikely()).to.be.false;
            await expect(await contract.enforceAgreement()).to.emit(contract, 'PenaltyApplied').withArgs(penaltyAmount, owner.address);

            await time.increase(SEVEN_DAYS_IN_SECONDS);

            // Penalty applied for failing in the second week
            expect(await contract.isPenaltyLikely()).to.be.true;
            await expect(await contract.enforceAgreement()).to.emit(contract, 'PenaltyApplied').withArgs(penaltyAmount, owner.address);

            // No penalty applied because the third week sucessed
            expect(await contract.isPenaltyLikely()).to.be.false;
            await expect(await contract.enforceAgreement()).to.emit(contract, 'NoPenaltyApplied');
        });
    });
});