package cash.p.terminal.modules.syncerror

import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.ISystemInfoManager

class SyncErrorService(
    private val wallet: Wallet,
    private val adapterManager: IAdapterManager,
    val reportEmail: String,
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val systemInfoManager: ISystemInfoManager,
) {

    val blockchainWrapper by lazy {
        btcBlockchainManager.blockchain(wallet.token.blockchainType)?.let {
            SyncErrorModule.BlockchainWrapper(it, SyncErrorModule.BlockchainWrapper.Type.Bitcoin)
        } ?: run {
            evmBlockchainManager.getBlockchain(wallet.token)?.let {
                SyncErrorModule.BlockchainWrapper(it, SyncErrorModule.BlockchainWrapper.Type.Evm)
            }
        }
    }

    val coinName: String = wallet.coin.name

    val sourceChangeable = blockchainWrapper != null

    fun retry() {
        adapterManager.refreshByWallet(wallet)
    }

    fun buildReportBody(error: String): String = buildString {
        appendLine(error)
        appendLine()
        appendLine("--- Diagnostic Info ---")
        appendLine("App Version: ${systemInfoManager.appVersionFull}")
        appendLine("Device: ${systemInfoManager.deviceModel}")
        appendLine("OS: ${systemInfoManager.osVersion}")
        appendLine("Coin: ${wallet.coin.code} (${wallet.coin.name})")
        appendLine("Token Type: ${wallet.token.type}")
        appendLine("Blockchain: ${wallet.token.blockchainType.uid}")
        appendLine("Account Type: ${wallet.account.type.description}")
        appendLine("Account Origin: ${wallet.account.origin.value}")
    }
}
