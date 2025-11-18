import { ethers } from 'ethers';
import type { ContractPhaseType, GetContractOverviewResponse, WeeklyGoal } from '../types';
import loadContract from './load-contract';

export async function getContractOverview(): Promise<GetContractOverviewResponse> {
    const {
        getBalance,
        executeMulticall,
        PhysicalActivityOracle,
        FitnessUnbreakableVow,
        TimeLordContract,
    } = await loadContract();
 
    const contractBalance = await getBalance(FitnessUnbreakableVow);

    const [
        startDate,
        expirationDate,
        currentWeekNumber,
        contractPhase,
        gracePeriod,
        secondsInAWeek,
        upkeeperCronSpec,
    ] = await executeMulticall(TimeLordContract, [
        'CREATION_DATE',
        'EXPIRATION_DATE',
        'getCurrentWeekIndex',
        'getContractPhase',
        'GRACE_PERIOD',
        'SECONDS_IN_ONE_WEEK',
        'END_OF_WEEK_CRON'
    ])

    const [
        oracleAddress,
        initialStakedAmount,
        penaltyAmount,
        allWeeksRaw,
        upkeeperAddress,
        upkeeperId,
        gymVisitsGoal,
        healthySleepNightsGoal,
        runningSessionsGoal,
        requiredNumberOfCompletedGoals,
        version,
    ] = await executeMulticall(FitnessUnbreakableVow, [
        "PHYSICAL_ACTIVITY_ORACLE",
        "STAKED_AMOUNT",
        "PENALTY_AMOUNT",
        "getAllWeeklyGoalsRecords",
        "CHAINLINK_UPKEEPER_ADDRESS",
        "CHAINLINK_UPKEEPER_ID",
        "GYM_VISITS_GOAL",
        "HEALTHY_SLEEP_NIGHTS_GOAL",
        "RUNNING_SESSIONS_GOAL",
        "REQUIRED_NUMBER_OF_COMPLETED_GOALS",
        "VERSION"
    ]);

    const [
        [, currentWeekPhysicalActivityStats],
        publicKeyAttestation,
        publicKey,
        sleepValidator,
        runningValidator,
        gymVisitValidator,
    ] = await executeMulticall(PhysicalActivityOracle, [
        'getCurrentWeekPhysicalActivityStats',
        'PUBLIC_KEY_ATTESTATION',
        'PUBLIC_KEY',
        'sleepValidator',
        'runningValidator',
        'gymVisitValidator',
    ]);

    const allWeeks = allWeeksRaw.map((weeklyGoal: Record<string, unknown>) => enrichWeeklyGoal(weeklyGoal)) as WeeklyGoal[];

    return {
        contractAddress: FitnessUnbreakableVow.contractAddress,
        oracleAddress,
        startDate: Number(startDate),
        expirationDate: Number(expirationDate),
        currentDate: Math.floor(Date.now() / 1000),
        initialStakedAmount: Number(ethers.formatEther(initialStakedAmount)),
        penaltyAmount: Number(ethers.formatEther(penaltyAmount)),
        currentBalance: Number(ethers.formatEther(contractBalance)),
        currentWeekNumber: Number(currentWeekNumber),
        currentWeekPhysicalActivityStats,
        allWeeks,
        upkeeperAddress,
        upkeeperCronSpec: upkeeperCronSpec,
        upkeeperId: upkeeperId,
        isContractExpired: Number(contractPhase) != 0,
        contractPhase: Number(contractPhase) as ContractPhaseType,
        gracePeriod: Number(gracePeriod),
        secondsInAWeek: Number(secondsInAWeek),
        network: FitnessUnbreakableVow.network,
        gymVisitsGoal: Number(gymVisitsGoal),
        runningSessionsGoal: Number(runningSessionsGoal),
        healthySleepNightsGoal: Number(healthySleepNightsGoal),
        requiredNumberOfCompletedGoals: Number(requiredNumberOfCompletedGoals),
        sleepValidator,
        runningValidator,
        version,
        gymVisitValidator,
        publicKeyInfo: {
            x: publicKey.x,
            y: publicKey.y,
            attestation: {
                challenge: publicKeyAttestation.attestationChallenge || '',
                sha256: publicKeyAttestation.attestationSha256 || '',
                cidFile: publicKeyAttestation.attestationIpfsCID || ''
            }
        }
    };
}

function enrichWeeklyGoal(weeklyGoal: Record<string, unknown>) {
    return {
        status: Number(weeklyGoal.status),
        goals: {
            gymVisitsGoalMet: weeklyGoal.wentoToTheGymEnoughTimes,
            run2KmGoalMet: weeklyGoal.ran2km,
            sleptWellGoalMet: weeklyGoal.sleptWell,
        },
        penaltyBlock: weeklyGoal.penaltyBlock ? String(weeklyGoal.penaltyBlock) : null,
    }
}

