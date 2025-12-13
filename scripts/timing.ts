export type WeekDurations = '3-minutes' | '5-minutes' | '3-hours' | '2-days' | '3-days' | '7-days';

// Round *up* to the next multiple of `stepMinutes` in UTC time.
function nextMultipleOfMinutes(stepMinutes: number, date: Date) {
    const d = new Date(date);

    const minutes = d.getUTCMinutes();
    const remainder = minutes % stepMinutes;
    const minutesToAdd = remainder === 0 ? stepMinutes : (stepMinutes - remainder);

    d.setUTCMinutes(minutes + minutesToAdd, 0, 0);

    return d;
}

// "Every 3 hours" schedule with a small safety buffer.
function buildThreeHourCron(date: Date, bufferMinutes = 10) {
    const d = new Date(date.getTime() + bufferMinutes * 60 * 1000);

    const minute = d.getUTCMinutes();
    const startHour = d.getUTCHours();

    const hours = Array.from({ length: 8 }, (_, idx) => (startHour + idx * 3) % 24);
    const uniqueSorted = [...new Set(hours)].sort((a, b) => a - b);

    return `${minute} ${uniqueSorted.join(",")} * * *`;
}

// Approximate "every 2 days" schedule: d0, d0+2, d0+4, d0+6
function buildTwoDayCron(date: Date, bufferHours = 3) {
    const d = new Date(date.getTime() + bufferHours * 60 * 60 * 1000);

    const minute = d.getUTCMinutes();
    const hour = d.getUTCHours();
    const dow = d.getUTCDay(); // 0–6, Sun–Sat

    const dows = [
        dow,
        (dow + 2) % 7,
        (dow + 4) % 7,
        (dow + 6) % 7,
    ];
    const uniqueSorted = [...new Set(dows)].sort((a, b) => a - b);

    return `${minute} ${hour} * * ${uniqueSorted.join(",")}`;
}

// Approximate "every 3 days" schedule: d0, d0+3, d0+6
function buildThreeDayCron(date: Date, bufferHours = 3) {
    const d = new Date(date.getTime() + bufferHours * 60 * 60 * 1000);

    const minute = d.getUTCMinutes();
    const hour = d.getUTCHours();
    const dow = d.getUTCDay();

    const dows = [
        dow,
        (dow + 3) % 7,
        (dow + 6) % 7,
    ];
    const uniqueSorted = [...new Set(dows)].sort((a, b) => a - b);

    return `${minute} ${hour} * * ${uniqueSorted.join(",")}`;
}

function buildWeeklyCron(date: Date, bufferHours: number = 3): string {
    const shifted = new Date(date.getTime() + bufferHours * 60 * 60 * 1000);

    const minute = shifted.getUTCMinutes();
    const hour   = shifted.getUTCHours();
    const dow    = shifted.getUTCDay();

    return `${minute} ${hour} * * ${dow}`;
}


export function getTimeSettings(startDate: string, secondsInOneWeek: WeekDurations) {
    const start = new Date(startDate);

    switch (secondsInOneWeek) {
        case '3-minutes': {
            return {
                startDate: Math.floor(nextMultipleOfMinutes(3, start).getTime() / 1000),
                cronUpkeeperSpec: '1,4,7,10,13,16,19,22,25,28,31,34,37,40,43,46,49,52,55,58 * * * *',   // (period=3min, buffer=1min)
                secondsInOneWeek: 180,
            };
        }

        case '5-minutes': {
            return {
                startDate: Math.floor(nextMultipleOfMinutes(5, start).getTime() / 1000),
                cronUpkeeperSpec: '2,7,12,17,22,27,32,37,42,47,52,57 * * * *',   // (period=5min, buffer=2min)
                secondsInOneWeek: 300,
            };
        }

        case '3-hours': {
            return {
                startDate: Math.floor(start.getTime() / 1000),
                cronUpkeeperSpec: buildThreeHourCron(start, 10), // (period=3h, buffer=10min)
                secondsInOneWeek: 10800,
            };
        }

        case '2-days': {
            return {
                startDate: Math.floor(start.getTime() / 1000),
                cronUpkeeperSpec: buildTwoDayCron(start, 3), // (period=2d, buffer=3h)
                secondsInOneWeek: 172800,
            };
        }

        case '3-days': {
            return {
                startDate: Math.floor(start.getTime() / 1000),
                cronUpkeeperSpec: buildThreeDayCron(start, 3), // (period=3d, buffer=3h)
                secondsInOneWeek: 259200,
            };
        }

        case '7-days': {
            return {
                startDate: Math.floor(start.getTime() / 1000),
                cronUpkeeperSpec: buildWeeklyCron(start, 4), // (period=7d, buffer=4h)
                secondsInOneWeek: 604800,
            };
        }

        default:
            throw new Error(`Unsupported secondsInOneWeek: ${secondsInOneWeek}`);
    }
}
