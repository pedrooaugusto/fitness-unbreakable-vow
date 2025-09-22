#!/bin/bash

set -euo pipefail

compileContracts() {
    echo "Compiling contracts."
    npx ts-node scripts/minify-verifier-script.ts
    npx hardhat compile
    echo -e "\n\n\n"
}

copyContractAbiToServer() {
    echo "Copying contract ABI files to backend."
    cp artifacts/contracts/FitnessUnbreakableVow.sol/FitnessUnbreakableVow.json @dashboard.server/src/abi/FitnessUnbreakableVow.json
    cp artifacts/contracts/PhysicalActivityOracle.sol/PhysicalActivityOracle.json @dashboard.server/src/abi/PhysicalActivityOracle.json
    echo -e "\n\n\n"
}

buildContractJavaClient() {
    echo "Building contract java clients with Web3j."
    node scripts/build-web3j-client.js
    echo -e "\n\n\n"
}

copyEnvVars() {
    echo "Copying env vars to Android app and Server."
    cp .env @dashboard.server/.env

    network="$1"

    if [ "$network" = "localhost" ]; then
        grep -E "^(${network}\.WALLET_PRIVATE_KEY|${network}\.RPC_URL)=" .env > android-app/app/.env
    else
        grep -E "^(${network}\.RPC_URL)=" .env > android-app/app/.env
        echo "${network}.WALLET_PRIVATE_KEY=" >> android-app/app/.env
    fi

    echo -e "\n\n\n"
}

case "$1" in
    build)
        echo "Running build..."
        compileContracts
        echo "Done."
        ;;
    build-prod)
        echo "Running production build..."
        compileContracts
        buildContractJavaClient
        copyContractAbiToServer
        copyEnvVars "$2"
        echo "Done."
        ;;
    deploy)
        echo "Deploying DeployPhysicalActivityOracle Contract..."
        verifyOracle=$(npx hardhat --network $2 DeployPhysicalActivityOracle | tail -n 1)

        if [ "$2" != "localhost" ]; then
            echo "Veryfing DeployPhysicalActivityOracle..."
            echo "$verifyOracle"
            timeout 45s bash -c "$verifyOracle" || true
        fi

        echo "Deploying DeployFitnessUnbreakableVow Contract..."
        verifyVow=$(npx hardhat --network $2 DeployFitnessUnbreakableVow | tail -n 1)

        if [ "$2" != "localhost" ]; then
            echo "Veryfing DeployFitnessUnbreakableVow..."
            echo "$verifyVow"
            timeout 45s bash -c "$verifyVow" || true
        fi

        echo "Copying new addresses to server and android app"
        cp contracts/.addresses @dashboard.server/src/abi/.addresses
        cp contracts/.addresses android-app/app/.addresses
        copyEnvVars "$2"
        echo "Done."
        ;;
    enforce)
        echo "Enforcing vow..."
        npx hardhat --network $2 EnforceVow
        echo "Done."
        ;;
    push-record)
        echo "Adding Physical Activity Record"
        npx hardhat --network "$2" PushPhysicalActivityRecord "${@:3}"
        ;;
    terminate)
        echo "Terminating vow"
        npx hardhat --network $2 TerminateVow
        echo "Done."
        ;;
    set-upkeep)
        echo "Set vow upkeep"
        npx hardhat --network "$2" SetUpkeepAddress "${@:3}"
        echo "Done."
        ;;
    chainlink)
        echo "Starting chainlink functions mock..."
        npx hardhat --network localhost MockChainLinkOracle
        ;;
    *)
        echo "Usage: $0 {build|build-prod|deploy|chainlink|enforce|terminate|push-record}"
        exit 1
        ;;
esac