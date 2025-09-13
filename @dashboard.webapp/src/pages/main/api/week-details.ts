import type { GetWeekDetailsResponse } from "../types";

export async function getWeekDetails(weekIndex: string): Promise<GetWeekDetailsResponse> {
    const response = await fetch(`http://localhost:3000/api/week-details/${weekIndex}`);

    const { body } = await response.json()

    console.log(body);

    return body as GetWeekDetailsResponse
}