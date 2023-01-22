package io.horizontalsystems.bankwallet.modules.watchaddress.selectblockchains

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.description
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.supports
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItem
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.CoinViewItemState
import io.horizontalsystems.bankwallet.modules.watchaddress.WatchAddressService
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class SelectBlockchainsViewModel(
    private val accountType: AccountType,
    private val accountName: String?,
    private val service: WatchAddressService,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val marketKit: MarketKitWrapper
) : ViewModel() {

    private var title: Int = R.string.Watch_Select_Blockchains
    private var blockchainViewItems = listOf<CoinViewItem<Blockchain>>()
    private var selectedBlockchains = setOf<Blockchain>()
    private var accountCreated = false

    var uiState by mutableStateOf(
        SelectBlockchainsUiState(
            title = title,
            blockchains = blockchainViewItems,
            submitButtonEnabled = true,
            accountCreated = false
        )
    )
        private set

    init {
        when (accountType) {
            is AccountType.Mnemonic,
            is AccountType.EvmPrivateKey,
            is AccountType.SolanaAddress -> Unit // N/A
            is AccountType.EvmAddress -> {
                title = R.string.Watch_Select_Blockchains
                val blockchains = evmBlockchainManager.allBlockchains
                blockchainViewItems = blockchains.map { blockchain ->
                    CoinViewItem(
                        item = blockchain,
                        imageSource = ImageSource.Remote(blockchain.type.imageUrl, R.drawable.ic_platform_placeholder_32),
                        title = blockchain.name,
                        subtitle = blockchain.description,
                        state = CoinViewItemState.ToggleVisible(false)
                    )
                }
            }
            is AccountType.HdExtendedKey -> {
                title = R.string.Watch_Select_Coins
                val blockchainTypes = listOf(BlockchainType.Bitcoin, BlockchainType.Dash, BlockchainType.BitcoinCash, BlockchainType.Litecoin)
                val supportedBlockchainTypeUids = blockchainTypes.filter { it.supports(accountType) }.map { it.uid }
                val blockchains = marketKit.blockchains(supportedBlockchainTypeUids)
                blockchainViewItems = blockchains.mapNotNull { blockchain ->
                    val tokenQuery = TokenQuery(blockchain.type, TokenType.Native)
                    marketKit.token(tokenQuery)?.let { token ->
                        CoinViewItem(
                            item = blockchain,
                            imageSource = ImageSource.Remote(token.fullCoin.coin.iconUrl, R.drawable.coin_placeholder),
                            title = token.fullCoin.coin.code,
                            subtitle = token.fullCoin.coin.name,
                            state = CoinViewItemState.ToggleVisible(enabled = false),
                            label = accountType.hdExtendedKey.info.purpose.name
                        )
                    }
                }
            }
        }

        emitState()
    }

    fun onToggleBlockchain(blockchain: Blockchain) {
        selectedBlockchains = if (selectedBlockchains.contains(blockchain))
            selectedBlockchains.toMutableSet().also { it.remove(blockchain) }
        else
            selectedBlockchains.toMutableSet().also { it.add(blockchain) }

        blockchainViewItems = blockchainViewItems.map { viewItem ->
            val enabled = selectedBlockchains.contains(viewItem.item)
            viewItem.copy(state = CoinViewItemState.ToggleVisible(enabled))
        }

        emitState()
    }

    fun onClickWatch() {
        service.watch(accountType, selectedBlockchains.toList(), accountName)
        accountCreated = true
        emitState()
    }

    private fun emitState() {
        uiState = SelectBlockchainsUiState(
            title = title,
            blockchains = blockchainViewItems,
            submitButtonEnabled = selectedBlockchains.isNotEmpty(),
            accountCreated = accountCreated
        )
    }
}

data class SelectBlockchainsUiState(
    val title: Int,
    val blockchains: List<CoinViewItem<Blockchain>>,
    val submitButtonEnabled: Boolean,
    val accountCreated: Boolean
)
