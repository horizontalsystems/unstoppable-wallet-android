package cash.p.terminal.wallet.useCases

import android.content.Context
import com.m2049r.xmrwallet.util.Helper
import com.m2049r.xmrwallet.util.KeyStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RemoveMoneroWalletFilesUseCase(private val appContext: Context) {

    suspend operator fun invoke(walletInnerName: String): Boolean = withContext(Dispatchers.IO) {
        val file = Helper.getWalletFile(appContext, walletInnerName);
        deleteWallet(file)
    }

    private fun deleteWallet(walletFile: File): Boolean {
        val dir = walletFile.getParentFile()
        val name = walletFile.getName()
        var success = true
        val cacheFile = File(dir, name)
        if (cacheFile.exists()) {
            success = cacheFile.delete()
        }
        success = File(dir, "$name.keys").delete() && success
        val addressFile = File(dir, "$name.address.txt")
        if (addressFile.exists()) {
            success = addressFile.delete() && success
        }
        KeyStoreHelper.removeWalletUserPass(appContext, walletFile.getName())
        return success
    }
}