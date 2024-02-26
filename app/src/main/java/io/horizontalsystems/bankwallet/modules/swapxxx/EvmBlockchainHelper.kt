package cash.p.terminal.modules.swapxxx
>>>>>>>> b890ba0e9 (Restructure classes):app/src/main/java/cash.p.terminal/modules/swapxxx/EvmBlockchainHelper.kt

import cash.p.terminal.core.App
import cash.p.terminal.core.ethereum.EvmCoinServiceFactory
import cash.p.terminal.modules.evmfee.EvmCommonGasDataService
import cash.p.terminal.modules.evmfee.EvmFeeService
import cash.p.terminal.modules.evmfee.Transaction
import cash.p.terminal.modules.evmfee.eip1559.Eip1559GasPriceService
import cash.p.terminal.modules.evmfee.legacy.LegacyGasPriceService
import cash.p.terminal.modules.send.SendModule
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.rx2.asFlow

class EvmBlockchainHelper(private val blockchainType: BlockchainType) {
    val evmKitWrapper = App.evmBlockchainManager
        .getEvmKitManager(blockchainType)
        .evmKitWrapper

    private val evmKit = evmKitWrapper?.evmKit

    val baseToken by lazy { App.evmBlockchainManager.getBaseToken(blockchainType) }
    val receiveAddress by lazy { evmKit?.receiveAddress }
    val chain by lazy { App.evmBlockchainManager.getChain(blockchainType) }

    private suspend fun getFeeData(transactionData: TransactionData): Transaction? {
        val evmKit = evmKit ?: return null

        val gasPriceService = if (chain.isEIP1559Supported) {
            val gasPriceProvider = Eip1559GasPriceProvider(evmKit)
            Eip1559GasPriceService(gasPriceProvider, evmKit)
        } else {
            val gasPriceProvider = LegacyGasPriceProvider(evmKit)
            LegacyGasPriceService(gasPriceProvider)
        }

        val gasDataService = EvmCommonGasDataService.instance(evmKit, blockchainType)
        val evmFeeService = EvmFeeService(evmKit, gasPriceService, gasDataService, transactionData)

        val transactionDataState = evmFeeService.transactionStatusObservable
            .asFlow()
            .firstOrNull { !it.loading }

        return transactionDataState?.dataOrNull
    }

    fun getRpcSourceHttp(): RpcSource.Http {
        val httpSyncSource = App.evmSyncSourceManager.getHttpSyncSource(blockchainType)
        val httpRpcSource = httpSyncSource?.rpcSource as? RpcSource.Http
            ?: throw IllegalStateException("No HTTP RPC Source for blockchain $blockchainType")
        return httpRpcSource
    }

    suspend fun getFeeAmountData(transactionData: TransactionData): SendModule.AmountData? {
        val baseToken = baseToken ?: return null
        val feeData = getFeeData(transactionData) ?: return null

        val coinServiceFactory = EvmCoinServiceFactory(
            baseToken,
            App.marketKit,
            App.currencyManager,
            App.coinManager
        )

        return coinServiceFactory.baseCoinService.amountData(
            feeData.gasData.estimatedFee,
            feeData.gasData.isSurcharged
        )
    }

}
