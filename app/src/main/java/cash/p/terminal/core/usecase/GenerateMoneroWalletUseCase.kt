package cash.p.terminal.core.usecase

import android.content.Context
import com.m2049r.xmrwallet.util.Helper

class GenerateMoneroWalletUseCase(
    private val appContext: Context
) {
    operator fun invoke(): String? {
        val walletName = generateWalletName()
        val walletFile = Helper.getWalletFile(appContext, walletName)

        return if (!walletFile.exists()) {
            return walletName
        } else {
            null
        }
    }

    private fun generateWalletName(): String {
        val timestamp = System.currentTimeMillis()
        return "wallet_$timestamp"
    }
}