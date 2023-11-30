package io.horizontalsystems.bankwallet.modules.watchaddress.selectblockchains

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.description
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.horizontalsystems.bankwallet.modules.watchaddress.WatchAddressService
import io.horizontalsystems.marketkit.models.Token

class SelectBlockchainsViewModel(
    private val accountType: AccountType,
    private val accountName: String?,
    private val service: WatchAddressService
) : ViewModel() {

    private var title: Int = R.string.Watch_Select_Blockchains
    private var coinViewItems = listOf<CoinViewItem<Token>>()
    private var selectedCoins = setOf<Token>()
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
        val tokens = service.tokens(accountType)
        selectedCoins = tokens.toSet()

        when (accountType) {
            is AccountType.SolanaAddress,
            is AccountType.TronAddress,
            is AccountType.BitcoinAddress,
            is AccountType.TonAddress,
            is AccountType.Cex,
            is AccountType.Mnemonic,
            is AccountType.EvmPrivateKey -> Unit // N/A
            is AccountType.EvmAddress -> {
                title = R.string.Watch_Select_Blockchains
                coinViewItems = tokens.map {
                    coinViewItemForBlockchain(it)
                }
            }

            is AccountType.HdExtendedKey -> {
                title = R.string.Watch_Select_Coins
                coinViewItems = tokens.map {
                    coinViewItemForToken(it, label = it.badge)
                }
            }
        }

        emitState()
    }

    private fun coinViewItemForBlockchain(token: Token): CoinViewItem<Token> {
        val blockchain = token.blockchain
        return CoinViewItem(
            item = token,
            imageSource = ImageSource.Remote(blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32),
            title = blockchain.name,
            subtitle = blockchain.description,
            enabled = true
        )
    }

    private fun coinViewItemForToken(token: Token, label: String?): CoinViewItem<Token> {
        return CoinViewItem(
            item = token,
            imageSource = ImageSource.Remote(token.fullCoin.coin.imageUrl, R.drawable.coin_placeholder),
            title = token.fullCoin.coin.code,
            subtitle = token.fullCoin.coin.name,
            enabled = true,
            label = label
        )
    }

    fun onToggle(token: Token) {
        selectedCoins = if (selectedCoins.contains(token))
            selectedCoins.toMutableSet().also { it.remove(token) }
        else
            selectedCoins.toMutableSet().also { it.add(token) }

        coinViewItems = coinViewItems.map { viewItem ->
            viewItem.copy(enabled = selectedCoins.contains(viewItem.item))
        }

        emitState()
    }

    fun onClickWatch() {
        service.watchTokens(accountType, selectedCoins.toList(), accountName)
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
    val coinViewItems: List<CoinViewItem<Token>>,
    val submitButtonEnabled: Boolean,
    val accountCreated: Boolean
)
