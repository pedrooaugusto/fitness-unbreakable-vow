import { API_HOST } from '../../utils';
import type { Currency, GetContractOverviewResponse } from '../types'


export async function getContractOverview(): Promise<GetContractOverviewResponse> {
    const response = await fetch(`${API_HOST}/overview`)

    const { body } = await response.json()

    console.log(body);

    return body as GetContractOverviewResponse
}

async function getConversionRates() {
    //const response = await fetch('https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=usd,brl')

    const data = {"ethereum":{"usd":4635.17,"brl":25082}} //await response.json()

    return { usd: data.ethereum.usd as number, brl: data.ethereum.brl as number, eth: 1 };
}

export async function convert(overview: GetContractOverviewResponse, currency: Currency = 'usd') {
    const exchangeRate = (await getConversionRates())[currency]

    return {
        ...overview,
        currentBalance: overview.currentBalance * exchangeRate,
        initialStakedAmount: overview.initialStakedAmount * exchangeRate,
        penaltyAmount: overview.penaltyAmount * exchangeRate,
        pastWeeksGoalsResult: overview.pastWeeksGoalsResult.map(item => {
            if (item.penaltyDetails == undefined) return item;
            const { penaltyDetails } = item;

            return {...item, penaltyDetails: {...penaltyDetails, amount: penaltyDetails.amount * exchangeRate} }
        })
    };
}