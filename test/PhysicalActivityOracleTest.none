import { time, loadFixture } from '@nomicfoundation/hardhat-toolbox/network-helpers';
import chai, { expect } from 'chai';
import chaiSubset from 'chai-subset';
import ChainlinkServer from '../scripts/chainlink-mock-server';
import { pushPhysicalActivityRecord } from './helpers/wait-utils';
import { recordEq } from './helpers/custom-chai-extensions';
import { signPhysicalActivityRecord } from '../scripts/utils';
import { deployContractFixture } from './helpers/deploy-contract-fixture';

chai.use(chaiSubset);
chai.use(recordEq);

describe("PhysicalActivityOracle", function () {
    this.beforeAll(() => ChainlinkServer.start());
    this.afterAll(() => ChainlinkServer.stop());

    describe("Read Physical Activity Records", function () {
        it("Should start with one empty record for the current week", async function () {
            const { physicalActivityOracle: contract } = await loadFixture(deployContractFixture);

            expect(await contract.listAllPhysicalActivityRecords()).to.have.lengthOf(1);
        });

        it("Should have one record for the first week", async function() {
            const { physicalActivityOracle: contract } = await loadFixture(deployContractFixture);
    
            const [currentWeek, record] = await contract.getCurrentWeekPhysicalActivityRecord();

            expect(currentWeek).to.be.eq(0n);
            expect(record).to.be.emptyRecord();
        });

        it("Should should have four records after four weeks", async function() {
            const { physicalActivityOracle: contract } = await loadFixture(deployContractFixture);
            await time.increase(4 * 7 * 24 * 60 * 60);

            const records = await contract.listAllPhysicalActivityRecords();
            const [weekNumber] = await contract.getCurrentWeekPhysicalActivityRecord();

            expect(weekNumber).to.be.eq(4);
            expect(records).to.have.lengthOf(5);
            records.forEach(record => expect(record).to.be.emptyRecord());
        });
    });

    
    describe("Write Physical Activity Records", function () {
        const recordToAdd1 = { timestamp: 0, runDistanceMeters: 2342, healthySleepNights: 3, gymVisits: 0 };
        const recordToAdd2 = { timestamp: 0, runDistanceMeters: 2352, healthySleepNights: 1, gymVisits: 3 };

        it("Should add a physical activity record", async function () {
            const { physicalActivityOracle: contract } = await loadFixture(deployContractFixture);

            await pushPhysicalActivityRecord(contract, recordToAdd1);

            const [, record] = await contract.getCurrentWeekPhysicalActivityRecord();
            expect(record).to.be.equalsRecord(recordToAdd1);
        });

        it("Should merge current record with latest data", async function () {
            const { physicalActivityOracle: contract } = await loadFixture(deployContractFixture);

            await pushPhysicalActivityRecord(contract, recordToAdd1);

            let [weekNumber, record] = await contract.getCurrentWeekPhysicalActivityRecord();
            expect(weekNumber).to.be.equals(0);
            expect(record).to.be.equalsRecord(recordToAdd1);

            await pushPhysicalActivityRecord(contract, recordToAdd2);

            [weekNumber, record] = await contract.getCurrentWeekPhysicalActivityRecord();
            expect(weekNumber).to.be.equals(0);
            expect(record).to.be.equalsRecord({ timestamp: 0n, runDistanceMeters: 2352n, healthySleepNights: 3, gymVisits: 3 });
        });

        it("Should store multiple activity records", async function () {
            const { physicalActivityOracle: contract } = await loadFixture(deployContractFixture);

            await pushPhysicalActivityRecord(contract, recordToAdd1);
            await time.increase(8 * 24 * 60 * 60);
            await pushPhysicalActivityRecord(contract, recordToAdd2);

            const records = await contract.listAllPhysicalActivityRecords();
            expect(records).to.have.lengthOf(2);
            expect(records[0]).to.be.equalsRecord(recordToAdd1);
            expect(records[1]).to.be.equalsRecord(recordToAdd2);
        });

        it("Should fail to store record with wrong signature", async function () {
            const { physicalActivityOracle: contract } = await loadFixture(deployContractFixture);
            const { signature: wrongSignature } = await signPhysicalActivityRecord(recordToAdd2);

            await expect(pushPhysicalActivityRecord(contract, recordToAdd1, wrongSignature)).to.be.rejected;
        });
    });
})
