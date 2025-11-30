import * as dotenv from 'dotenv';
import { ScheduledHandler } from 'aws-lambda';
import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';
import { loadContracts, parsePhysicalActivityStatsUpdateEvent } from './contract';
import { getBlockNumber, getEvents } from './etherscan';
import { requireEnv } from './helpers';

dotenv.config();

export const handler: ScheduledHandler = async (event: any, context) => {
    const { timeLord, physicalActivityOracleAddress, network } = await loadContracts(event?.contractAddress, event?.network);

    const currentWeekIndex = event?.currentWeekIndex || Number(await timeLord.getCurrentWeekIndex());
    const previousWeek = currentWeekIndex - 1;

    if (previousWeek < 0) {
        console.log('[INFO] Still in week #0, nothing to do.');

        return;
    }

    console.log('[INFO] Fetching events for week #' + previousWeek);

    const secondsInOneWeek = Number(await timeLord.SECONDS_IN_ONE_WEEK());
    const creationDate = Number(await timeLord.CREATION_DATE());

    const { weekStart, weekEnd } = getWeekStartAndEnd(creationDate, previousWeek, secondsInOneWeek);

    console.log(`[INFO] Retrieving block numbers for timestamps: ${weekStart} and ${weekEnd}`);

    const weekStartBlockNumber = await getBlockNumber(weekStart, network);
    const weekEndBlockNumber = await getBlockNumber(weekEnd, network);

    console.log(`[INFO] Querying events between blocks ${weekStartBlockNumber} and ${weekEndBlockNumber}`);

    const events = await getEvents(physicalActivityOracleAddress, network, previousWeek, weekStartBlockNumber, weekEndBlockNumber);

    console.log('[INFO] Trying to parse '+ events.length +' events returned by Etherscan.' );

    const physicalActivityStatsUpdate = JSON.stringify(parsePhysicalActivityStatsUpdateEvent(events), (_, value) => typeof value === 'bigint' ? Number(value) : value);

    console.log('[INFO] Saving events to S3: ', physicalActivityStatsUpdate);

    await saveToS3(physicalActivityStatsUpdate, `events/${physicalActivityOracleAddress}/week-${previousWeek}/PhysicalActivityStatsUpdate.json`);

    console.log('[INFO] Completed.');
}

function getWeekStartAndEnd(creationDate: number, weekIndex: number, secondsInOneWeek: number) {
    const weekStart = creationDate + weekIndex * secondsInOneWeek;
    const weekEnd = weekStart + secondsInOneWeek - 1;

    return { weekStart, weekEnd };
}

async function saveToS3(body: string, key: string) {
    const bucket = requireEnv('BUCKET');
    const region = process.env['AWS_REGION'] || process.env['AWS_DEFAULT_REGION'] || 'us-east-1';

    const s3 = new S3Client({ region });

    await s3.send(new PutObjectCommand({ Bucket: bucket, Key: key, Body: body, ContentType: 'application/json' }));
}
