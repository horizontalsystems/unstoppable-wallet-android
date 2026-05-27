package cash.p.terminal.modules.multiswap.sendtransaction

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.EvmError
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.ServiceState
import cash.p.terminal.core.ethereum.CautionViewItem
import cash.p.terminal.core.managers.LocallyCreatedTransactionRepository
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.useCases.WalletUseCase
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.entities.CurrencyValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal
import java.net.UnknownHostException
import java.util.UUID

abstract class ISendTransactionService<T>(protected val token: Token) :
    ServiceState<SendTransactionServiceState>() {
    open val mevProtectionAvailable: Boolean = false
    protected var extraFees = mapOf<FeeType, SendModule.AmountData>()
    protected val walletUseCase: WalletUseCase by inject(WalletUseCase::class.java)
    protected val wallet: Wallet by lazy { runBlocking { walletUseCase.createWalletIfNotExists(token)!! } }
    protected val adapterManager: IAdapterManager by inject(IAdapterManager::class.java)
    protected val adapter: T by lazy {
        runBlocking {
            adapterManager.awaitAdapterForWallet(wallet)
                ?: throw IllegalStateException("Adapter not available for ${token.coin.code}")
        }
    }
    private val baseCurrency = App.currencyManager.baseCurrency
    protected var uuid = UUID.randomUUID().toString()
    private val marketKit: MarketKitWrapper by inject(MarketKitWrapper::class.java)
    protected val numberFormatter: IAppNumberFormatter by inject(IAppNumberFormatter::class.java)
    private val locallyCreatedTransactionRepository: LocallyCreatedTransactionRepository by inject(
        LocallyCreatedTransactionRepository::class.java
    )

    protected val rate: CurrencyValue?
        get() {
            val currencyManager: CurrencyManager by inject(CurrencyManager::class.java)
            val baseCurrency = currencyManager.baseCurrency
            return marketKit.coinPrice(feeToken.coin.uid, baseCurrency.code)?.let {
                CurrencyValue(baseCurrency, it.value)
            }
        }

    protected val feeToken: Token
        get() = marketKit.token(TokenQuery(token.blockchainType, TokenType.Native)) ?: token

    protected val coroutineScope = CoroutineScope(Dispatchers.IO)

    abstract fun hasSettings(): Boolean
    abstract fun start(coroutineScope: CoroutineScope)
    abstract suspend fun setSendTransactionData(data: SendTransactionData)

    @Composable
    abstract fun GetSettingsContent(navController: NavController)

    abstract suspend fun sendTransaction(mevProtectionEnabled: Boolean = false): SendTransactionResult
    abstract val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings>

    private fun getRate(coin: Coin): CurrencyValue? {
        return App.marketKit.coinPrice(coin.uid, baseCurrency.code)?.let {
            CurrencyValue(baseCurrency, it.value)
        }
    }

    protected fun getAmountData(coinValue: CoinValue): SendModule.AmountData {
        val primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)

        val secondaryAmountInfo = getRate(coinValue.coin)?.let { rate ->
            SendModule.AmountInfo.CurrencyValueInfo(
                CurrencyValue(
                    rate.currency,
                    rate.value * coinValue.value
                )
            )
        }
        return SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)
    }

    protected fun setExtraFeesMap(feesMap: Map<FeeType, CoinValue>) {
        extraFees = feesMap.mapValues { (_, coinValue) ->
            getAmountData(coinValue)
        }
    }

    protected suspend fun markTransactionCreated(transactionHash: String?) {
        locallyCreatedTransactionRepository.markCreated(wallet, transactionHash)
    }

    protected fun createCaution(
        error: Throwable,
        type: CautionViewItem.Type = CautionViewItem.Type.Error
    ) = when (error) {
        is UnknownHostException -> CautionViewItem(
            TranslatableString.ResString(R.string.Hud_Text_NoInternet).toString(),
            "",
            type
        )

        is LocalizedException -> CautionViewItem(
            TranslatableString.ResString(error.errorTextRes, *error.formatArgs).toString(),
            "",
            type
        )

        is EvmError.InsufficientBalanceWithFee -> CautionViewItem(
            Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalanceWithFee, feeToken.coin.code),
            "",
            type
        )

        else -> CautionViewItem(
            TranslatableString.PlainString(error.message ?: "").toString(),
            "",
            type
        )
    }
}

data class SendTransactionServiceState(
    val availableBalance: BigDecimal?,
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val feeCaution: CautionViewItem? = null,
    val sendable: Boolean,
    val loading: Boolean,
    val fields: List<DataField>,
    val extraFees: Map<FeeType, SendModule.AmountData> = emptyMap()
)
