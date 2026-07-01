package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.runBlocking

class ZcashBirthdayProvider(
    private val context: Context,
) {
    fun getLatestCheckpointBlockHeight(): Long {
        val walletBirthday = runBlocking {
            BlockHeight.ofLatestCheckpoint(context, ZcashNetwork.Mainnet)
        }
        return walletBirthday.value
    }
}
