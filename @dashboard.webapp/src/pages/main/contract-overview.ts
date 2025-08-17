import type { GetContractOverviewResponse } from './types'

export async function getContractOverview(): Promise<GetContractOverviewResponse> {
    const response = await fetch('http://localhost:3000/api/overview')

    const { body } = await response.json()

    console.log(body);

    return body as GetContractOverviewResponse
}

async function getConversionRates() {
    //const response = await fetch('https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=usd,brl')

    const data = {"ethereum":{"usd":4635.17,"brl":25082}} //await response.json()

    return { usd: data.ethereum.usd as number, brl: data.ethereum.brl as number, eth: 1 };
}

export async function convert(overview: GetContractOverviewResponse, currency: 'usd' | 'brl' | 'eth' = 'usd') {
    const exchangeRate = (await getConversionRates())[currency]

    return {
        ...overview,
        currentBalance: overview.currentBalance * exchangeRate,
        initialStakedAmount: overview.initialStakedAmount * exchangeRate,
        penaltyAmount: overview.penaltyAmount * exchangeRate
    }
}