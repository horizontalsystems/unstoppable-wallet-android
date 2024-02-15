package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItemFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.modules.evmfee.EvmCommonGasDataService
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeService
import io.horizontalsystems.bankwallet.modules.evmfee.IEvmGasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.eip1559.Eip1559GasPriceService
import io.horizontalsystems.bankwallet.modules.evmfee.legacy.LegacyGasPriceService
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData.AdditionalInfo
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData.WalletConnectInfo
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceService
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmSettingsService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WCChainData
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toLong
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigInteger

object WCRequestModule {

    class FactoryV2(
        private val evmKitWrapper: EvmKitWrapper,
        private val transaction: WalletConnectTransaction,
        peerName: String
    ) : ViewModelProvider.Factory {

        private val chain: WCChainData by lazy {
            val ethereumKit = evmKitWrapper.evmKit
            val chain = ethereumKit.chain
            val address = ethereumKit.receiveAddress.eip55.shorten()
            WCChainData(chain, address)
        }

        private val blockchainType = when (evmKitWrapper.evmKit.chain) {
            Chain.BinanceSmartChain -> BlockchainType.BinanceSmartChain
            Chain.Polygon -> BlockchainType.Polygon
            Chain.Avalanche -> BlockchainType.Avalanche
            Chain.Optimism -> BlockchainType.Optimism
            Chain.ArbitrumOne -> BlockchainType.ArbitrumOne
            Chain.Gnosis -> BlockchainType.Gnosis
            Chain.Fantom -> BlockchainType.Fantom
            else -> BlockchainType.Ethereum
        }
        private val token by lazy {
            getToken(blockchainType)
        }
        private val transactionData =
            TransactionData(
                transaction.to,
                transaction.value,
                transaction.data
            )

        private val gasPrice by lazy { getGasPrice(transaction) }

        private val gasPriceService by lazy {
            getGasPriceService(gasPrice, evmKitWrapper.evmKit)
        }

        private val coinServiceFactory by lazy {
            EvmCoinServiceFactory(
                token,
                App.marketKit,
                App.currencyManager,
                App.coinManager
            )
        }
        private val feeService by lazy {
            val gasDataService = EvmCommonGasDataService.instance(
                evmKitWrapper.evmKit,
                evmKitWrapper.blockchainType
            )
            EvmFeeService(evmKitWrapper.evmKit, gasPriceService, gasDataService, transactionData)
        }
        private val cautionViewItemFactory by lazy { CautionViewItemFactory(coinServiceFactory.baseCoinService) }
        private val additionalInfo =
            AdditionalInfo.WalletConnectRequest(WalletConnectInfo(peerName, chain))
        private val nonceService by lazy {
            SendEvmNonceService(
                evmKitWrapper.evmKit,
                transaction.nonce
            )
        }
        private val settingsService by lazy { SendEvmSettingsService(feeService, nonceService) }

        private val sendService by lazy {
            SendEvmTransactionService(
                SendEvmData(transactionData, additionalInfo),
                evmKitWrapper,
                settingsService,
                App.evmLabelManager
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                WCSendEthereumTransactionRequestViewModel::class.java -> {
                    WCSendEthereumTransactionRequestViewModel() as T
                }

                EvmFeeCellViewModel::class.java -> {
                    EvmFeeCellViewModel(
                        feeService,
                        gasPriceService,
                        coinServiceFactory.baseCoinService
                    ) as T
                }

                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(
                        sendService,
                        coinServiceFactory,
                        cautionViewItemFactory,
                        blockchainType = blockchainType,
                        contactsRepo = App.contactsRepository
                    ) as T
                }

                SendEvmNonceViewModel::class.java -> {
                    SendEvmNonceViewModel(nonceService) as T
                }

                else -> throw IllegalArgumentException()
            }
        }
    }

    private fun getToken(blockchainType: BlockchainType): Token {
        return App.evmBlockchainManager.getBaseToken(blockchainType)!!
    }

    private fun getGasPrice(transaction: WalletConnectTransaction): GasPrice? = when {
        transaction.maxFeePerGas != null && transaction.maxPriorityFeePerGas != null -> {
            GasPrice.Eip1559(transaction.maxFeePerGas, transaction.maxPriorityFeePerGas)
        }

        else -> {
            transaction.gasPrice?.let { GasPrice.Legacy(it) }
        }
    }

    private fun getGasPriceService(gasPrice: GasPrice?, evmKit: EthereumKit): IEvmGasPriceService {
        return when {
            gasPrice is GasPrice.Legacy || gasPrice == null && !evmKit.chain.isEIP1559Supported -> {
                val gasPriceProvider = LegacyGasPriceProvider(evmKit)
                LegacyGasPriceService(
                    gasPriceProvider,
                    initialGasPrice = (gasPrice as? GasPrice.Legacy)?.legacyGasPrice
                )
            }

            else -> {
                val gasPriceProvider = Eip1559GasPriceProvider(evmKit)
                Eip1559GasPriceService(
                    gasPriceProvider,
                    evmKit,
                    initialGasPrice = gasPrice as? GasPrice.Eip1559
                )
            }
        }
    }

}

data class WalletConnectTransaction(
    val from: Address,
    val to: Address,
    val nonce: Long?,
    val gasPrice: Long?,
    val gasLimit: Long?,
    val maxPriorityFeePerGas: Long?,
    val maxFeePerGas: Long?,
    val value: BigInteger,
    val data: ByteArray
)

data class WCEthereumTransaction(
    val from: String,
    val to: String?,
    val nonce: String?,
    val gasPrice: String?,
    val gas: String?,
    val gasLimit: String?,
    val maxPriorityFeePerGas: String?,
    val maxFeePerGas: String?,
    val value: String?,
    val data: String
){
    fun getWCTransaction(): WalletConnectTransaction {
        val transaction = this
        val to = transaction.to
        checkNotNull(to) {
            throw TransactionError.NoRecipient()
        }

        return WalletConnectTransaction(
            from = Address(transaction.from),
            to = Address(to),
            nonce = transaction.nonce?.hexStringToByteArray()?.toLong(),
            gasPrice = transaction.gasPrice?.hexStringToByteArray()?.toLong(),
            gasLimit = (transaction.gas ?: transaction.gasLimit)?.hexStringToByteArray()?.toLong(),
            maxPriorityFeePerGas = transaction.maxPriorityFeePerGas?.hexStringToByteArray()?.toLong(),
            maxFeePerGas = transaction.maxFeePerGas?.hexStringToByteArray()?.toLong(),
            value = transaction.value?.hexStringToByteArray()?.toBigInteger() ?: BigInteger.ZERO,
            data = transaction.data.hexStringToByteArray()
        )
    }

    sealed class TransactionError : Exception() {
        class NoRecipient : TransactionError()
    }
}
