package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import cash.z.ecc.android.sdk.type.ZcashNetwork

class ZcashBirthdayProvider(
        private val context: Context,
        testMode: Boolean
) {
    private val network = if (testMode) ZcashNetwork.Testnet else ZcashNetwork.Mainnet
    fun getNearestBirthdayHeight(birthdayHeight: Long? = null): Long {
        val walletBirthday = WalletBirthdayTool.loadNearest(context, network, birthdayHeight?.toInt())
        return walletBirthday.height.toLong()
    }

    @Throws
    fun validateBirthdayHeight(birthdayHeight: Long) {
        WalletBirthdayTool.loadNearest(context, network, birthdayHeight.toInt())
    }

}
