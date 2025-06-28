package cash.p.terminal.core.usecase

import android.content.Context
import cash.p.terminal.core.utils.MoneroConfig
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AccountType.MnemonicMonero
import com.m2049r.xmrwallet.data.NodeInfo
import com.m2049r.xmrwallet.model.Wallet
import com.m2049r.xmrwallet.model.WalletManager
import com.m2049r.xmrwallet.util.Helper
import com.m2049r.xmrwallet.util.KeyStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class GenerateMoneroWalletUseCase(private val appContext: Context) {
    suspend operator fun invoke(): AccountType? = withContext(Dispatchers.IO) {
        val walletInnerName = generateUniqueWalletName() ?: return@withContext null

        // check if the wallet we want to create already exists
        val walletFolder: File = Helper.getWalletRoot(appContext)
        if (!walletFolder.isDirectory()) {
            Timber.e("Wallet dir " + walletFolder.getAbsolutePath() + "is not a directory")
            return@withContext null
        }
        val cacheFile = File(walletFolder, walletInnerName)
        val keysFile = File(walletFolder, "$walletInnerName.keys")
        val addressFile = File(walletFolder, "$walletInnerName.address.txt")

        if (cacheFile.exists() || keysFile.exists() || addressFile.exists()) {
            Timber.e("Some wallet files already exist for %s", cacheFile.absolutePath)
            return@withContext null
        }

        val newWalletFile = File(walletFolder, walletInnerName)
        val crazyPass = KeyStoreHelper.getCrazyPass(appContext, "")
        val currentNode: NodeInfo = NodeInfo.fromString(MoneroConfig.defaultNode.uri)

        // get it from the connected node if we have one
        val restoreHeight = if (currentNode.testRpcService()) {
            currentNode.height
        } else {
            -1
        }
        val newWallet = WalletManager.getInstance()
            .createWallet(
                newWalletFile,
                crazyPass,
                "English",
                restoreHeight
            )
        val walletStatus: Wallet.Status = newWallet.status
        if (!walletStatus.isOk) {
            Timber.e(walletStatus.errorString)
        }
        val words = newWallet.getSeed("").split(" ")
        newWallet.close()

        return@withContext if (walletStatus.isOk) {
            MnemonicMonero(
                words = words,
                password = crazyPass,
                walletInnerName = walletInnerName
            )
        } else {
            null
        }
    }

    private fun generateUniqueWalletName(): String? {
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