package cash.p.terminal.modules.coin.overview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cash.p.terminal.R
import cash.p.terminal.core.*
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.*
import cash.p.terminal.modules.coin.CoinViewFactory
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.disposables.CompositeDisposable

class CoinOverviewViewModel(
    private val service: CoinOverviewService,
    private val factory: CoinViewFactory,
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager
) : ViewModel() {

    val isRefreshingLiveData = MutableLiveData<Boolean>(false)
    val overviewLiveData = MutableLiveData<CoinOverviewViewItem>()
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)

    var xxxTokens by mutableStateOf<List<XxxTokenInfo>>(listOf())
        private set

    private val disposables = CompositeDisposable()

    private var fullCoin = service.fullCoin
    private var activeAccount = accountManager.activeAccount
    private var activeWallets = walletManager.activeWallets

    init {
        service.coinOverviewObservable
            .subscribeIO { coinOverview ->
                isRefreshingLiveData.postValue(false)

                coinOverview.dataOrNull?.let {
                    overviewLiveData.postValue(factory.getOverviewViewItem(it))
                }

                coinOverview.viewState?.let {
                    viewStateLiveData.postValue(it)
                }
            }
            .let {
                disposables.add(it)
            }

        service.start()

        walletManager.activeWalletsUpdatedObservable
            .subscribeIO {
                activeWallets = it
                refreshXxxTokensInfo()
            }
            .let {
                disposables.add(it)
            }

        refreshXxxTokensInfo()
    }

    private fun refreshXxxTokensInfo() {
        xxxTokens = getTokenInfo(fullCoin, activeAccount, activeWallets)
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    fun refresh() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }

    fun retry() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }

    private fun getTokenInfo(fullCoin: FullCoin, account: Account?, activeWallets: List<Wallet>): List<XxxTokenInfo> {
        val items = mutableListOf<XxxTokenInfo>()

        val accountTypeNotWatch = if (account != null && !account.isWatchAccount) {
            account.type
        } else {
            null
        }

        fullCoin.tokens.sortedBy { it.blockchainType.order }.forEach { token ->
            val canAddToWallet = accountTypeNotWatch != null
                && token.isSupported
                && token.blockchainType.supports(accountTypeNotWatch)

            when (token.blockchainType.coinSettingType) {
                CoinSettingType.derivation -> {
                    AccountType.Derivation.values().forEach { derivation ->
                        val coinSettings =
                            CoinSettings(mapOf(CoinSettingType.derivation to derivation.value))
                        val configuredToken = ConfiguredToken(token, coinSettings)
                        val inWallet =
                            canAddToWallet && activeWallets.any { it.configuredToken == configuredToken }
                        items.add(
                            XxxTokenInfo(
                                rawValue = derivation.addressType,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = null,
                                name = derivation.rawName,
                                configuredToken = configuredToken,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet,
                            )
                        )
                    }
                }
                CoinSettingType.bitcoinCashCoinType -> {
                    BitcoinCashCoinType.values().forEach { bchCoinType ->
                        val coinSettings =
                            CoinSettings(mapOf(CoinSettingType.bitcoinCashCoinType to bchCoinType.value))
                        val configuredToken = ConfiguredToken(token, coinSettings)
                        val inWallet =
                            canAddToWallet && activeWallets.any { it.configuredToken == configuredToken }
                        items.add(
                            XxxTokenInfo(
                                rawValue = Translator.getString(bchCoinType.title),
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = null,
                                name = bchCoinType.value,
                                configuredToken = configuredToken,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet
                            )
                        )
                    }
                }
                null -> when (val tokenType = token.type) {
                    is TokenType.Eip20 -> {
                        val configuredToken = ConfiguredToken(token)
                        val inWallet =
                            canAddToWallet && activeWallets.any { it.configuredToken == configuredToken }
                        items.add(
                            XxxTokenInfo(
                                rawValue = tokenType.address,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = explorerUrl(token, tokenType.address),
                                name = token.blockchain.name,
                                configuredToken = configuredToken,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet
                            )
                        )
                    }
                    is TokenType.Bep2 -> {
                        val configuredToken = ConfiguredToken(token)
                        val inWallet =
                            canAddToWallet && activeWallets.any { it.configuredToken == configuredToken }
                        items.add(
                            XxxTokenInfo(
                                rawValue = tokenType.symbol,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = explorerUrl(token, tokenType.symbol),
                                name = token.blockchain.name,
                                configuredToken = configuredToken,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet
                            )
                        )
                    }
                    TokenType.Native -> {
                        val configuredToken = ConfiguredToken(token)
                        val inWallet =
                            canAddToWallet && activeWallets.any { it.configuredToken == configuredToken }
                        items.add(
                            XxxTokenInfo(
                                rawValue = Translator.getString(R.string.CoinPlatforms_Native),
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = null,
                                name = token.blockchain.name,
                                configuredToken = configuredToken,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet
                            )
                        )
                    }
                    is TokenType.Spl -> {
                        val configuredToken = ConfiguredToken(token)
                        val inWallet =
                            canAddToWallet && activeWallets.any { it.configuredToken == configuredToken }
                        items.add(
                            XxxTokenInfo(
                                rawValue = tokenType.address,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = explorerUrl(token, tokenType.address),
                                name = token.blockchain.name,
                                configuredToken = configuredToken,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet
                            )
                        )
                    }
                    is TokenType.Unsupported -> Unit
                }
            }
        }

        return items
    }

    private fun explorerUrl(token: Token, reference: String) : String? {
        return token.blockchain.explorerUrl?.replace("\$ref", reference)
    }

}
