import { ethers } from 'ethers';
import { requireEnv } from './helpers';
import { abi as PhysicalActivityOracleAbi } from './abi/PhysicalActivityOracle.json';
import { abi as FitnessUnbreakableVowAbi } from './abi/FitnessUnbreakableVow.json';
import { abi as TimeLordAbi } from './abi/TheDoctor.json';

export async function loadContracts(physicalActivityOracleAddress?: string, network?: string) {
    if (physicalActivityOracleAddress == null || network == null) throw new Error('Contract address and network must be provided.');

    const rpcUrl = requireEnv(network + '_RPC_PROVIDER');
    const provider = new ethers.JsonRpcProvider(rpcUrl);
    const signer = new ethers.Wallet(requireEnv(`${network}_WALLET_PRIVATE_KEY`), provider);
    const physicalActivityOracle = new ethers.Contract(physicalActivityOracleAddress, PhysicalActivityOracleAbi, signer);
    const timeLordAddress = await physicalActivityOracle.TIME_LORD();
    const fitVowAddress = await physicalActivityOracle.FITNESS_UNBREAKABLE_VOW();
    const timeLord = new ethers.Contract(timeLordAddress, TimeLordAbi, signer);
    const fitnessUnbreakableVow = new ethers.Contract(fitVowAddress, FitnessUnbreakableVowAbi, signer);

    return { physicalActivityOracle, timeLord, fitnessUnbreakableVow, physicalActivityOracleAddress, timeLordAddress, network, provider };
}
