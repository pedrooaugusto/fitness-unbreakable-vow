package com.august.fitnessvowsync.mapper

import javax.inject.Inject
import javax.inject.Named

class BlockExplorerUrlMapper @Inject constructor(@Named("NETWORK") private val network: String) {
    fun toTransactionUrl(transactionHash: String): String {
        if (network == "SEPOLIA") return "http://sepolia.etherscan.io/tx/${transactionHash}"

        return "https://www.arbiscan.io/tx/${transactionHash}"
    }
}