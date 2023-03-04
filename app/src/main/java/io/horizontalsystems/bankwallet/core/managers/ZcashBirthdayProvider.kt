package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.runBlocking

class ZcashBirthdayProvider(
    private val context: Context,
    testMode: Boolean
) {
    private val network = if (testMode) ZcashNetwork.Testnet else ZcashNetwork.Mainnet
    fun getLatestCheckpointBlockHeight(): Long {
        val walletBirthday = runBlocking {
            BlockHeight.ofLatestCheckpoint(context, network)
        }
        return walletBirthday.value
    }
}
