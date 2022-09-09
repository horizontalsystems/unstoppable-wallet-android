package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import cash.z.ecc.android.sdk.type.ZcashNetwork
import kotlinx.coroutines.runBlocking

class ZcashBirthdayProvider(
    private val context: Context,
    testMode: Boolean
) {
    private val network = if (testMode) ZcashNetwork.Testnet else ZcashNetwork.Mainnet
    fun getNearestBirthdayHeight(birthdayHeight: Int? = null): Int {
        val walletBirthday = runBlocking {
            WalletBirthdayTool.loadNearest(context, network, birthdayHeight)
        }
        return walletBirthday.height
    }

    @Throws
    fun validateBirthdayHeight(birthdayHeight: Int) {
        runBlocking {
            WalletBirthdayTool.loadNearest(context, network, birthdayHeight)
        }
    }

}
