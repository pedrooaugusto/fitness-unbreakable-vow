import * as dotenv from 'dotenv';
import { ScheduledHandler } from 'aws-lambda';
import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';
import { EventTopic, loadContracts, parseGymVisitEventProcessed, parsePhysicalActivityStatsUpdateEvent, parseRunningEventProcessed, parseSleepEventProcessed } from './contract';
import { getBlockNumber, getEvents } from './etherscan';
import { requireEnv } from './helpers';

dotenv.config();

export const handler: ScheduledHandler = async (event: any, context) => {
    const { timeLord, physicalActivityOracleAddress, network } = await loadContracts(event?.contractAddress, event?.network);

    const currentWeekIndex = event?.currentWeekIndex || Number(await timeLord.getCurrentWeekIndex());

    console.log('[INFO] Fetching events for week #' + currentWeekIndex);
    const secondsInOneWeek = Number(await timeLord.SECONDS_IN_ONE_WEEK());
    const creationDate = Number(await timeLord.CREATION_DATE());

    const { weekStart, weekEnd } = getWeekStartAndEnd(creationDate, currentWeekIndex, secondsInOneWeek);

    console.log(`[INFO] Retrieving block numbers for timestamps: ${weekStart} and ${weekEnd}`);
    const weekStartBlockNumber = await getBlockNumber(weekStart, network);
    const weekEndBlockNumber = await getBlockNumber(weekEnd, network);

    await sleep(4000); // 3-calls per second limit

    console.log(`[INFO] Querying events between blocks ${weekStartBlockNumber} and ${weekEndBlockNumber}`);
    const statsUpdateEvent = await getEvents(physicalActivityOracleAddress, EventTopic.PhysicalActivityStatsUpdate, network, currentWeekIndex, weekStartBlockNumber, weekEndBlockNumber);
    const gymVisitEvent = await getEvents(physicalActivityOracleAddress, EventTopic.GymVisitEventProcessed, network, currentWeekIndex, weekStartBlockNumber, weekEndBlockNumber);

    await sleep(4000); // 3-calls per second limit

    const runningEvent = await getEvents(physicalActivityOracleAddress, EventTopic.RunningEventProcessed, network, currentWeekIndex, weekStartBlockNumber, weekEndBlockNumber);
    const sleepEvent = await getEvents(physicalActivityOracleAddress, EventTopic.SleepEventProcessed, network, currentWeekIndex, weekStartBlockNumber, weekEndBlockNumber);

    console.log('[INFO] Trying to parse `statsUpdateEvent` '+ statsUpdateEvent.length +' events returned by Etherscan.' );
    const statsUpdateEventStr = JSON.stringify(parsePhysicalActivityStatsUpdateEvent(statsUpdateEvent), bigIntNormalizer);

    console.log('[INFO] Trying to parse `gymVisitEvent` '+ gymVisitEvent.length +' events returned by Etherscan.' );
    const gymVisitEventStr = JSON.stringify(parseGymVisitEventProcessed(gymVisitEvent), bigIntNormalizer);

    console.log('[INFO] Trying to parse `runningEvent` '+ runningEvent.length +' events returned by Etherscan.' );
    const runningEventStr = JSON.stringify(parseRunningEventProcessed(runningEvent), bigIntNormalizer);

    console.log('[INFO] Trying to parse `sleepEvent` '+ sleepEvent.length +' events returned by Etherscan.' );
    const sleepEventStr = JSON.stringify(parseSleepEventProcessed(sleepEvent), bigIntNormalizer);

    console.log('[INFO] Saving statsUpdateEventStr to S3: ', statsUpdateEventStr);
    await saveToS3(statsUpdateEventStr, `events/${physicalActivityOracleAddress}/week-${currentWeekIndex}/PhysicalActivityStatsUpdate.json`);
    
    console.log('[INFO] Saving gymVisitEventStr to S3: ', gymVisitEventStr);
    await saveToS3(gymVisitEventStr, `events/${physicalActivityOracleAddress}/week-${currentWeekIndex}/GymVisitEventProcessed.json`);
    
    console.log('[INFO] Saving runningEventStr to S3: ', runningEventStr);
    await saveToS3(runningEventStr, `events/${physicalActivityOracleAddress}/week-${currentWeekIndex}/RunningEventProcessed.json`);

    console.log('[INFO] Saving sleepEventStr to S3: ', sleepEventStr);
    await saveToS3(sleepEventStr, `events/${physicalActivityOracleAddress}/week-${currentWeekIndex}/SleepEventProcessed.json`);

    console.log('[INFO] Completed.');
}

function getWeekStartAndEnd(creationDate: number, weekIndex: number, secondsInOneWeek: number) {
    const now = Math.floor(+new Date() / 1000) - 60; 
    const weekStart = creationDate + weekIndex * secondsInOneWeek;
    const weekEnd = Math.min(weekStart + secondsInOneWeek - 1, now);

    return { weekStart, weekEnd };
}

async function saveToS3(body: string, key: string) {
    const bucket = requireEnv('BUCKET');
    const region = process.env['AWS_REGION'] || process.env['AWS_DEFAULT_REGION'] || 'us-east-1';

    const s3 = new S3Client({ region });

    await s3.send(new PutObjectCommand({ Bucket: bucket, Key: key, Body: body, ContentType: 'application/json', CacheControl: 'public, max-age=5400' }));
}

const bigIntNormalizer = (_: unknown, value: unknown) => typeof value === 'bigint' ? Number(value) : value

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))