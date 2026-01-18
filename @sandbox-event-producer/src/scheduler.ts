import {
    GetScheduleCommand,
    SchedulerClient,
    UpdateScheduleCommand,
    type UpdateScheduleCommandInput,
} from '@aws-sdk/client-scheduler';
import { requireEnv } from './helpers';

const DEFAULT_REGION = 'us-east-1';

type ScheduleSyncOptions = {
    startDate: Date;
    endDate: Date;
    inputOverrides?: Record<string, unknown>;
};

export async function syncEventBridgeSchedule(options: ScheduleSyncOptions) {
    const scheduleName = requireEnv('SCHEDULER_NAME');
    const region = process.env['AWS_REGION'] || process.env['AWS_DEFAULT_REGION'] || DEFAULT_REGION;
    const client = new SchedulerClient({ region });

    const current = await client.send(new GetScheduleCommand({ Name: scheduleName }));

    if (!current.ScheduleExpression) {
        throw new Error('Missing ScheduleExpression on scheduler.');
    }

    if (!current.Target || !current.Target.RoleArn) {
        throw new Error('Missing Target.RoleArn on scheduler.');
    }

    if (!current.FlexibleTimeWindow) {
        throw new Error('Missing FlexibleTimeWindow on scheduler.');
    }

    const target = {
        ...current.Target,
        Input: mergeInput(current.Target.Input, options.inputOverrides),
    };

    const update: UpdateScheduleCommandInput = {
        Name: scheduleName,
        ScheduleExpression: current.ScheduleExpression,
        FlexibleTimeWindow: current.FlexibleTimeWindow,
        Target: target,
        StartDate: options.startDate,
        EndDate: options.endDate,
    };

    if (current.Description) update.Description = current.Description;
    if (current.GroupName) update.GroupName = current.GroupName;
    if (current.KmsKeyArn) update.KmsKeyArn = current.KmsKeyArn;
    if (current.ScheduleExpressionTimezone) update.ScheduleExpressionTimezone = current.ScheduleExpressionTimezone;
    if (current.State) update.State = current.State;

    console.log(`[INFO] Setting shceduler start and end date to: `, update);

    await client.send(new UpdateScheduleCommand(update));
}

function mergeInput(existingInput: string | undefined, overrides?: Record<string, unknown>) {
    if (!overrides || Object.keys(overrides).length === 0) return existingInput;

    let base: Record<string, unknown> = {};

    if (existingInput) {
        try {
            base = JSON.parse(existingInput);
        } catch {
            base = {};
        }
    }

    return JSON.stringify({ ...base, ...overrides });
}
