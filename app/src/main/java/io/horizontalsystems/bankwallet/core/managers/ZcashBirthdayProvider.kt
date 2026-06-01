package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZcashBirthdayProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getLatestCheckpointBlockHeight(): Long {
        val walletBirthday = runBlocking {
            BlockHeight.ofLatestCheckpoint(context, ZcashNetwork.Mainnet)
        }
        return walletBirthday.value
    }
}
