import type { Currency, GetContractOverviewResponse } from '../types';

async function getConversionRates() {
    let rates = null;

    try {
        const response = await fetch('https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=usd,brl');

        rates = await response.json();

    } catch (err) {
        console.warn('Unable to fetch actual USD/ETH conversion rates. Using rates from August 2025', err);

        // It's fine, realistically, how much it can vary...
        rates = { "ethereum": { "usd" : 4635.17, "brl":25082 }}
    }

    return { usd: rates.ethereum.usd as number, brl: rates.ethereum.brl as number, eth: 1 };
}

export async function convert(overview: GetContractOverviewResponse, currency: Currency = 'usd'): Promise<GetContractOverviewResponse> {
    const exchangeRate = (await getConversionRates())[currency];

    return {
        ...overview,
        currentBalance: overview.currentBalance * exchangeRate,
        initialStakedAmount: overview.initialStakedAmount * exchangeRate,
        penaltyAmount: overview.penaltyAmount * exchangeRate,
    };
}