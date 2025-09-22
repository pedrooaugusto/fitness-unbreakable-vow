import { API_HOST } from "../../utils";
import type { GetWeekDetailsResponse } from "../types";

export async function getWeekDetails(weekIndex: string): Promise<GetWeekDetailsResponse> {
    const response = await fetch(`${API_HOST}/week-details/${weekIndex}`);

    const { body } = await response.json()

    console.log(body);

    return body as GetWeekDetailsResponse
}