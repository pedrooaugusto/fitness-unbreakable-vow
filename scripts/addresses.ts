import path from 'path';
import fs from 'fs';
import * as dotenv from 'dotenv';

export function getContractAddress(contractName: string, network: string) {
    const addressesFile = path.join(__dirname, '..', "contracts", ".addresses");

    return dotenv.parse(fs.readFileSync(addressesFile, "utf8"))[`${network}.${contractName}`];
}

export function saveContractAddress(contractName: string, contractAddress: string, network: string) {
    const addressesFile = path.join(__dirname, '..', "contracts", ".addresses");
    const deployedAddresses = dotenv.parse(fs.readFileSync(addressesFile, "utf8"));

    deployedAddresses[`${network}.${contractName}`] = contractAddress;

    deployedAddresses['LastUsedNetwork'] = network;

    const deployedAddressesString = Object
        .entries(deployedAddresses)
        .map(([key, value]) => `${key}=${value}`)
        .join("\n");

    fs.writeFileSync(addressesFile, deployedAddressesString);
}