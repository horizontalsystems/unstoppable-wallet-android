package io.horizontalsystems.bankwallet.modules.watchaddress.selectblockchains

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.description
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.ConfiguredToken
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.horizontalsystems.bankwallet.modules.watchaddress.WatchAddressService

class SelectBlockchainsViewModel(
    private val accountType: AccountType,
    private val accountName: String?,
    private val service: WatchAddressService
) : ViewModel() {

    private var title: Int = R.string.Watch_Select_Blockchains
    private var coinViewItems = listOf<CoinViewItem<ConfiguredToken>>()
    private var selectedCoins = setOf<ConfiguredToken>()
    private var accountCreated = false

    var uiState by mutableStateOf(
        SelectBlockchainsUiState(
            title = title,
            coinViewItems = coinViewItems,
            submitButtonEnabled = true,
            accountCreated = false
        )
    )
        private set

    init {
        when (accountType) {
            is AccountType.Mnemonic,
            is AccountType.EvmPrivateKey,
            is AccountType.SolanaAddress,
            is AccountType.TronAddress -> Unit // N/A
            is AccountType.EvmAddress -> {
                title = R.string.Watch_Select_Blockchains
                coinViewItems = service.configuredTokens(accountType).map {
                    coinViewItemForBlockchain(it)
                }
            }
            is AccountType.HdExtendedKey -> {
                title = R.string.Watch_Select_Coins
                coinViewItems = service.configuredTokens(accountType).map {
                    coinViewItemForToken(it, label = it.coinSettings.settings.values.firstOrNull())
                }
            }
        }

        emitState()
    }

    private fun coinViewItemForBlockchain(configuredToken: ConfiguredToken): CoinViewItem<ConfiguredToken> {
        val blockchain = configuredToken.token.blockchain
        return CoinViewItem(
            item = configuredToken,
            imageSource = ImageSource.Remote(blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32),
            title = blockchain.name,
            subtitle = blockchain.description,
            enabled = false
        )
    }

    private fun coinViewItemForToken(configuredToken: ConfiguredToken, label: String?): CoinViewItem<ConfiguredToken> {
        val token = configuredToken.token
        return CoinViewItem(
            item = configuredToken,
            imageSource = ImageSource.Remote(token.fullCoin.coin.imageUrl, R.drawable.coin_placeholder),
            title = token.fullCoin.coin.code,
            subtitle = token.fullCoin.coin.name,
            enabled = false,
            label = label
        )
    }

    fun onToggle(configuredToken: ConfiguredToken) {
        selectedCoins = if (selectedCoins.contains(configuredToken))
            selectedCoins.toMutableSet().also { it.remove(configuredToken) }
        else
            selectedCoins.toMutableSet().also { it.add(configuredToken) }

        coinViewItems = coinViewItems.map { viewItem ->
            viewItem.copy(enabled = selectedCoins.contains(viewItem.item))
        }

        emitState()
    }

    fun onClickWatch() {
        service.watchConfiguredTokens(accountType, selectedCoins.toList(), accountName)
        accountCreated = true
        emitState()
    }

    private fun emitState() {
        uiState = SelectBlockchainsUiState(
            title = title,
            coinViewItems = coinViewItems,
            submitButtonEnabled = selectedCoins.isNotEmpty(),
            accountCreated = accountCreated
        )
    }
}

data class SelectBlockchainsUiState(
    val title: Int,
    val coinViewItems: List<CoinViewItem<ConfiguredToken>>,
    val submitButtonEnabled: Boolean,
    val accountCreated: Boolean
)
