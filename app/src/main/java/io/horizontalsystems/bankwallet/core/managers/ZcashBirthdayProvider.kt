package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool

class ZcashBirthdayProvider(
        private val context: Context
) {
    fun getNearestBirthdayHeight(birthdayHeight: Long? = null): Long {
        val walletBirthday = WalletBirthdayTool.loadNearest(context, birthdayHeight?.toInt())
        return walletBirthday.height.toLong()
    }

    @Throws
    fun validateBirthdayHeight(birthdayHeight: Long) {
        WalletBirthdayTool.loadNearest(context, birthdayHeight.toInt())
    }

}
