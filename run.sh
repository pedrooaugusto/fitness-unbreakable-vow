#!/bin/bash

set -euo pipefail

compileContracts() {
    echo "Compiling contracts."
    npx hardhat compile
    echo -e "\n\n\n"
}

copyContractAbiToFrontend() {
    echo "Copying contract ABI files to frontend."
    cp artifacts/contracts/FitnessUnbreakableVow.sol/FitnessUnbreakableVow.json @website/public/abi/FitnessUnbreakableVow.json
    cp artifacts/contracts/PhysicalActivityOracle.sol/PhysicalActivityOracle.json @website/public/abi/PhysicalActivityOracle.json
    echo -e "\n\n\n"
}

buildContractJavaClient() {
    echo "Building contract java clients with Web3j."
    node scripts/build-web3j-client.js
    echo -e "\n\n\n"
}

copyWalletDetailsToAndroidApp() {
    echo "Copying RPC URL and Wallet Private Key to Android app."

    network="$1"

    if [ "$network" = "localhost" ]; then
        grep -E "^(${network}\.WALLET_PRIVATE_KEY|${network}\.RPC_URL)=" .env > @androidapp/app/.env
    else
        grep -E "^(${network}\.RPC_URL)=" .env > @androidapp/app/.env
        echo "${network}.WALLET_PRIVATE_KEY=" >> @androidapp/app/.env
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
        copyContractAbiToFrontend
        copyWalletDetailsToAndroidApp "$2"
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

        echo "Copying new addresses to webapp and android app"
        cp contracts/.addresses @website/public/addresses
        cp contracts/.addresses @androidapp/app/.addresses
        copyWalletDetailsToAndroidApp "$2"
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
    deploy-webapp)
        echo "Deploying frontend"

        BUCKET="s3://e2e4fa73"

        # (A) Update content & deletions (no headers here)
        aws s3 sync @website/dist "$BUCKET" --delete

        # (B) Long-cache everything EXCEPT the no-cache files (force metadata)
        aws s3 cp @website/dist "$BUCKET" --recursive \
            --exclude "abi/*" \
            --exclude "addresses" \
            --exclude "index.html" \
            --exclude "logo.svg" \
            --exclude "robots.txt" \
            --cache-control "public, max-age=31536000, immutable" \
            --metadata-directive REPLACE

        # (C) No-cache for the special set (force metadata)
        aws s3 cp @website/dist "$BUCKET" --recursive \
            --exclude "*" \
            --include "abi/*" \
            --include "addresses" \
            --include "index.html" \
            --include "logo.svg" \
            --include "robots.txt" \
            --cache-control "no-cache, must-revalidate" \
            --metadata-directive REPLACE

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