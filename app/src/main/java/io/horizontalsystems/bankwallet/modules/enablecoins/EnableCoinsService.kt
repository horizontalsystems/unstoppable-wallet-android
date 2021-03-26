package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.IBuildConfigProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class EnableCoinsService(
        appConfigProvider: IBuildConfigProvider,
        private val ethereumProvider: EnableCoinsErc20Provider,
        private val bep2Provider: EnableCoinsBep2Provider,
        private val bep20Provider: EnableCoinsBep20Provider,
        private val coinManager: ICoinManager) {

    val testMode = appConfigProvider.testMode
    val enableCoinsAsync = PublishSubject.create<List<Coin>>()
    val stateAsync = BehaviorSubject.createDefault<State>(State.Idle)

    var state: State = State.Idle
        private set(value) {
            field = value
            stateAsync.onNext(value)
        }

    private var disposables = CompositeDisposable()

    fun handle(coinType: CoinType, accountType: AccountType) {
        val tokenType = resolveTokenType(coinType, accountType) ?: return
        state = State.WaitingForApprove(tokenType)
    }

    fun approveEnable() {
        val state = state
        if (state !is State.WaitingForApprove) {
            return
        }

        this.state = State.Loading
        disposables.clear()

        try {
            when (state.tokenType) {
                is TokenType.Erc20 -> fetchErc20Tokens(state.tokenType.words)
                is TokenType.Bep2 -> fetchBep2Tokens(state.tokenType.words)
                is TokenType.Bep20 -> fetchBep20Tokens(state.tokenType.words)
            }
        } catch (err: Throwable) {
            this.state = State.Failure(err)
        }
    }

    private fun resolveTokenType(coinType: CoinType, accountType: AccountType): TokenType? {
        return when {
            accountType !is AccountType.Mnemonic -> null
            coinType is CoinType.Ethereum && accountType.words.size == 12 -> TokenType.Erc20(accountType.words)
            coinType is CoinType.BinanceSmartChain && accountType.words.size == 24 -> TokenType.Bep20(accountType.words)
            coinType is CoinType.Bep2 && coinType.symbol == "BNB" && accountType.words.size == 24 -> TokenType.Bep2(accountType.words)
            else -> null
        }
    }

    private fun fetchBep20Tokens(words: List<String>) {
        val address = EthereumKit.address(words, EthereumKit.NetworkType.BscMainNet)

        bep20Provider.getTokenAddressesAsync(address.hex)
                .subscribeIO({ coins ->
                    handleFetchBep20(coins)
                }, {
                    state = State.Failure(it)
                })
                .let { disposables.add(it) }
    }

    private fun handleFetchBep20(addresses: List<String>) {
        val allCoins = coinManager.coins

        val coins = addresses.mapNotNull { address ->
            allCoins.firstOrNull { it.type is CoinType.Bep20 && (it.type as CoinType.Bep20).address.equals(address, ignoreCase = true) }
        }

        state = State.Success(coins)
        enableCoinsAsync.onNext(coins)
    }

    private fun fetchErc20Tokens(words: List<String>) {
        val networkType = if (testMode) EthereumKit.NetworkType.EthRopsten else EthereumKit.NetworkType.EthMainNet
        val address = EthereumKit.address(words, networkType)

        ethereumProvider.getTokenAddressesAsync(address.hex)
                .subscribeIO({ tokenAddresses ->
                    handleFetchErc20(tokenAddresses)
                }, {
                    state = State.Failure(it)
                })
                .let { disposables.add(it) }
    }

    private fun handleFetchErc20(addresses: List<String>) {
        val allCoins = coinManager.coins

        val coins = addresses.mapNotNull { address ->
            allCoins.firstOrNull { it.type is CoinType.Erc20 && (it.type as CoinType.Erc20).address.equals(address, ignoreCase = true) }
        }

        state = State.Success(coins)
        enableCoinsAsync.onNext(coins)
    }

    private fun fetchBep2Tokens(words: List<String>) {
        bep2Provider.getTokenSymbolsAsync(words)
                .subscribeIO({ coins ->
                    handleFetchBep2(coins)
                }, {
                    state = State.Failure(it)
                })
                .let { disposables.add(it) }
    }

    private fun handleFetchBep2(tokenSymbols: List<String>) {
        val allCoins = coinManager.coins

        val coins = tokenSymbols.mapNotNull { symbol ->
            if (symbol.equals("BNB", ignoreCase = true)) {
                allCoins.firstOrNull { it.type is CoinType.Bep2 && (it.type as CoinType.Bep2).symbol.equals(symbol, ignoreCase = true) }
            } else {
                null
            }
        }

        state = State.Success(coins)
        enableCoinsAsync.onNext(coins)
    }

    sealed class State {
        object Idle : State()
        class WaitingForApprove(val tokenType: TokenType) : State()
        object Loading : State()
        class Success(val coins: List<Coin>) : State()
        class Failure(val error: Throwable) : State()
    }

    sealed class TokenType {
        class Erc20(val words: List<String>) : TokenType()
        class Bep2(val words: List<String>) : TokenType()
        class Bep20(val words: List<String>) : TokenType()

        val title: String
            get() = when (this) {
                is Erc20 -> "ERC20"
                is Bep2 -> "BEP2"
                is Bep20 -> "BEP20"
            }
    }
}
