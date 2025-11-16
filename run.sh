#!/bin/bash

set -euo pipefail

copyContractAbiToFrontend() {
    echo "Copying contract ABI files to frontend."
    cp artifacts/contracts/FitnessUnbreakableVow.sol/FitnessUnbreakableVow.json @website/public/abi/FitnessUnbreakableVow.json
    cp artifacts/contracts/PhysicalActivityOracle.sol/PhysicalActivityOracle.json @website/public/abi/PhysicalActivityOracle.json
    cp artifacts/contracts/TheDoctor.sol/TheDoctor.json @website/public/abi/TheDoctor.json
    echo "Copying contract ABI files to event store."
    cp artifacts/contracts/PhysicalActivityOracle.sol/PhysicalActivityOracle.json @contract-event-store/src/abi/PhysicalActivityOracle.json
    cp artifacts/contracts/TheDoctor.sol/TheDoctor.json @contract-event-store/src/abi/TheDoctor.json
    echo -e "\n\n\n"
}

buildContractJavaClient() {
    echo "Building contract java clients with Web3j."
    node scripts/build-web3j-client.js
    echo -e "\n\n\n"
}

case "$1" in
    build)
        echo "Building contracts..."
        npx hardhat compile --network "$2"
        echo "Done."
        ;;
    build-prod)
        echo "Building all artifacts..."
        npx hardhat compile --network "$2"
        buildContractJavaClient
        copyContractAbiToFrontend
        echo "Done."
        ;;
    deploy)
        network="$2"

        # Strip subcommand and network from arguments, leaving only deploy options
        shift 2

        echo "Building artifacts."
        npx hardhat compile --network "$network"

        # Optional staked amount (others go straight to Hardhat)
        vow_args=()
        oracle_args=()

        # Parse deploy options (all optional)
        while [ "$#" -gt 0 ]; do
            case "$1" in
                --stakedamount)
                    vow_args+=("$1" "$2")
                    shift 2
                    ;;
                *)
                    oracle_args+=("$1")
                    shift
                    ;;
            esac
        done

        echo "Deploying DeployPhysicalActivityOracle Contract..."
        npx hardhat DeployPhysicalActivityOracle --network "$network" "${oracle_args[@]}"

        echo "Deploying DeployFitnessUnbreakableVow Contract..."
        npx hardhat DeployFitnessUnbreakableVow --network "$network" "${vow_args[@]}"

        echo "Copying new addresses to webapp and android app"
        cp contracts/.addresses @website/public/addresses
        cp contracts/.addresses @androidapp/app/.addresses
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
    build-androidapp)
        cp contracts/.addresses @androidapp/app/.addresses
        gradle -p @androidapp :app:assembleRelease
        unsigned_apk=@androidapp/app/build/outputs/apk/release/app-release-unsigned.apk
        @androidapp/app/amnesiac-apk-signer.sh sign-and-forget $unsigned_apk -o artifacts/signed-release-app.apk
        ;;
    *)
        echo "Usage: $0 {build|build-prod|deploy|enforce|terminate|push-record|deploy-webapp}"
        exit 1
        ;;
esac
