package io.horizontalsystems.bankwallet.modules.coin.overview

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.accountTypeDerivation
import io.horizontalsystems.bankwallet.core.assetUrl
import io.horizontalsystems.bankwallet.core.bitcoinCashCoinType
import io.horizontalsystems.bankwallet.core.eip20TokenUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.isSupported
import io.horizontalsystems.bankwallet.core.jettonUrl
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.core.supports
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.bankwallet.modules.coin.CoinViewFactory
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class CoinOverviewViewModel(
    private val service: CoinOverviewService,
    private val factory: CoinViewFactory,
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager,
    private val chartIndicatorManager: ChartIndicatorManager
) : ViewModel() {

    val isRefreshingLiveData = MutableLiveData<Boolean>(false)
    val overviewLiveData = MutableLiveData<CoinOverviewViewItem>()
    val viewStateLiveData = MutableLiveData<ViewState>(ViewState.Loading)

    var tokenVariants by mutableStateOf<TokenVariants?>(null)
        private set
    var showHudMessage by mutableStateOf<HudMessage?>(null)
        private set

    var chartIndicatorsState by mutableStateOf(
        ChartIndicatorsState(
            hasActiveSubscription = true,
            enabled = chartIndicatorManager.isEnabled
        )
    )

    private var hudMessage: HudMessage? = null
        set(value) {
            field = value
            showHudMessage = value
        }
    val fullCoin = service.fullCoin
    private var activeAccount = accountManager.activeAccount
    private var activeWallets = walletManager.activeWallets

    init {
        viewModelScope.launch {
            service.coinOverviewObservable.asFlow().collect { coinOverview ->
                isRefreshingLiveData.postValue(false)

                coinOverview.dataOrNull?.let {
                    overviewLiveData.postValue(factory.getOverviewViewItem(it))
                }

                coinOverview.viewState?.let {
                    viewStateLiveData.postValue(it)
                }
            }
        }

        service.start()

        viewModelScope.launch {
            walletManager.activeWalletsUpdatedObservable.asFlow().collect { wallets ->
                if (wallets.size > activeWallets.size) {
                    hudMessage = HudMessage(R.string.Hud_Added_To_Wallet, HudMessageType.Success, R.drawable.ic_add_to_wallet_2_24)
                } else if (wallets.size < activeWallets.size) {
                    hudMessage = HudMessage(R.string.Hud_Removed_From_Wallet, HudMessageType.Error, R.drawable.ic_empty_wallet_24)
                }

                activeWallets = wallets
                refreshTokensVariants()
            }
        }

        refreshTokensVariants()
    }

    fun enableChartIndicators() {
        chartIndicatorManager.enable()
        chartIndicatorsState = chartIndicatorsState.copy(enabled = true)
    }

    fun disableChartIndicators() {
        chartIndicatorManager.disable()
        chartIndicatorsState = chartIndicatorsState.copy(enabled = false)
    }

    fun onHudMessageShown() {
        hudMessage = null
    }

    private fun refreshTokensVariants() {
        tokenVariants = getTokenVariants(fullCoin, activeAccount, activeWallets)
    }

    override fun onCleared() {
        service.stop()
    }

    fun refresh() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }

    fun retry() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }

    private fun getTokenVariants(fullCoin: FullCoin, account: Account?, activeWallets: List<Wallet>): TokenVariants? {
        val items = mutableListOf<TokenVariant>()
        var type = TokenVariants.Type.Blockchains

        val accountTypeNotWatch = if (account != null && !account.isWatchAccount) {
            account.type
        } else {
            null
        }

        fullCoin.tokens
            .filter { when(val tokenType = it.type){
                is TokenType.Unsupported -> tokenType.reference.isNotBlank()
                else -> true
            } }
            .sortedWith(
            compareBy<Token> { it.type.order }
                .thenBy { it.blockchainType.order }
        )
            .forEach { token ->
                val canAddToWallet = accountTypeNotWatch != null
                        && token.isSupported
                        && token.blockchainType.supports(accountTypeNotWatch)

                when (val tokenType = token.type) {
                    is TokenType.Asset -> {
                        val inWallet =
                            canAddToWallet && activeWallets.any { it.token == token }
                        val id = "${tokenType.code}:${tokenType.issuer}"

                        items.add(
                            TokenVariant(
                                value = id.shorten(),
                                copyValue = id,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = token.blockchain.assetUrl(tokenType.code, tokenType.issuer),
                                name = token.blockchain.name,
                                token = token,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet
                            )
                        )
                    }
                    is TokenType.Jetton -> {
                        val inWallet =
                            canAddToWallet && activeWallets.any { it.token == token }
                        items.add(
                            TokenVariant(
                                value = tokenType.address.shorten(),
                                copyValue = tokenType.address,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = token.blockchain.jettonUrl(tokenType.address),
                                name = token.blockchain.name,
                                token = token,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet
                            )
                        )
                    }
                    is TokenType.Eip20 -> {
                        val inWallet =
                            canAddToWallet && activeWallets.any { it.token == token }
                        items.add(
                            TokenVariant(
                                value = tokenType.address.shorten(),
                                copyValue = tokenType.address,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = token.blockchain.eip20TokenUrl(tokenType.address),
                                name = token.blockchain.name,
                                token = token,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet
                            )
                        )
                    }

                    is TokenType.Spl -> {
                        val inWallet =
                            canAddToWallet && activeWallets.any { it.token == token }
                        items.add(
                            TokenVariant(
                                value = tokenType.address.shorten(),
                                copyValue = tokenType.address,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = token.blockchain.eip20TokenUrl(tokenType.address),
                                name = token.blockchain.name,
                                token = token,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet
                            )
                        )
                    }

                    is TokenType.Derived -> {
                        type = TokenVariants.Type.Bips

                        val derivation = tokenType.derivation.accountTypeDerivation

                        val inWallet =
                            canAddToWallet && activeWallets.any { it.token == token }
                        items.add(
                            TokenVariant(
                                value = derivation.addressType + derivation.recommended,
                                copyValue = null,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = null,
                                name = derivation.rawName,
                                token = token,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet,
                            )
                        )
                    }

                    is TokenType.AddressTyped -> {
                        type = TokenVariants.Type.CoinTypes

                        val bchCoinType = tokenType.type.bitcoinCashCoinType

                        val inWallet =
                            canAddToWallet && activeWallets.any { it.token == token }
                        items.add(
                            TokenVariant(
                                value = bchCoinType.title,
                                copyValue = null,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = null,
                                name = bchCoinType.value,
                                token = token,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet
                            )
                        )
                    }

                    TokenType.Native -> {
                        val inWallet =
                            canAddToWallet && activeWallets.any { it.token == token }
                        items.add(
                            TokenVariant(
                                value = Translator.getString(R.string.CoinPlatforms_Native),
                                copyValue = null,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = null,
                                name = token.blockchain.name,
                                token = token,
                                canAddToWallet = canAddToWallet,
                                inWallet = inWallet
                            )
                        )
                    }

                    is TokenType.Unsupported -> {
                        items.add(
                            TokenVariant(
                                value = tokenType.reference.shorten(),
                                copyValue = tokenType.reference,
                                imgUrl = token.blockchainType.imageUrl,
                                explorerUrl = when {
                                    tokenType.reference.isNotBlank() -> token.blockchain.eip20TokenUrl(
                                        tokenType.reference
                                    )

                                    else -> null
                                },
                                name = token.blockchain.name,
                                token = token,
                                canAddToWallet = false,
                                inWallet = false
                            )
                        )
                    }
                }
            }

        return when {
            items.isNotEmpty() -> TokenVariants(items, type)
            else -> null
        }
    }

}

data class ChartIndicatorsState(val hasActiveSubscription: Boolean, val enabled: Boolean)

data class TokenVariants(val items: List<TokenVariant>, val type: Type) {
    enum class Type(@StringRes val titleResId: Int) {
        Blockchains(R.string.CoinPage_Blockchains),
        Bips(R.string.CoinPage_Bips),
        CoinTypes(R.string.CoinPage_CoinTypes)
    }
}
