import * as dotenv from 'dotenv';
import { ScheduledHandler } from 'aws-lambda';
import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';
import { loadContract, parseEvent } from './contract';
import { getBlockNumber, getEvents, LogEntry } from './etherscan';
import { requireEnv } from './helpers';

dotenv.config();

export const handler: ScheduledHandler = async (event: any, context) => {
    const { contract, address } = loadContract(event?.contractAddress);

    const currentWeekIndex = event?.currentWeekIndex || Number(await contract.getCurrentWeekIndex());
    const previousWeek = currentWeekIndex - 1;

    if (previousWeek < 0) {
        console.log('[INFO] Still in week #0, nothing to do.');

        return;
    }

    console.log('[INFO] Fetching events for week #' + previousWeek);

    const secondsInOneWeek = Number(await contract.SECONDS_IN_ONE_WEEK());
    const creationDate = Number(await contract.CREATION_DATE());

    const { weekStart, weekEnd } = getWeekStartAndEnd(creationDate, previousWeek, secondsInOneWeek);

    console.log(`[INFO] Retrieving block numbers for timestamps: ${weekStart} and ${weekEnd}`);

    const weekStartBlockNumber = await getBlockNumber(weekStart);
    const weekEndBlockNumber = await getBlockNumber(weekEnd);

    console.log(`[INFO] Querying events between blocks ${weekStartBlockNumber} and ${weekEndBlockNumber}`);

    const events = await getEvents(address, previousWeek, weekStartBlockNumber, weekEndBlockNumber);
    const physicalActivityRecords = decodeEvents(events);

    console.log('[INFO] Saving events to S3: ', physicalActivityRecords);

    await saveToS3(JSON.stringify(physicalActivityRecords), `events/${address}/week-${previousWeek}/PhysicalActivityRecordProcessed.json`);
}


function getWeekStartAndEnd(creationDate: number, weekIndex: number, secondsInOneWeek: number) {
    const weekStart = creationDate + weekIndex * secondsInOneWeek;
    const weekEnd = weekStart + secondsInOneWeek - 1;

    return { weekStart, weekEnd };
}


function decodeEvents(events: LogEntry[]) {
    if (!events || events.length === 0) return [] as Array<{ weekIndex: number; runDistanceMeters: number; gymVisits: number; healthySleepNights: number; transactionHash: string }>;

    return events.map(evt => parseEvent(evt.topics[0], evt.topics[1], evt.data, evt.transactionHash, Number(evt.blockNumber)));
}

async function saveToS3(body: string, key: string) {
    const bucket = requireEnv('BUCKET');
    const region = process.env['AWS_REGION'] || process.env['AWS_DEFAULT_REGION'] || 'us-east-1';

    const s3 = new S3Client({ region });

    await s3.send(new PutObjectCommand({ Bucket: bucket, Key: key, Body: body, ContentType: 'application/json' }));
}
