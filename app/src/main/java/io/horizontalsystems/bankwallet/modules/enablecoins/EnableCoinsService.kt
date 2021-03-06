package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.IBuildConfigProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class EnableCoinsService(
        private val appConfigProvider: IBuildConfigProvider,
        private val ethereumProvider: EnableCoinsErc20Provider,
        private val bep2Provider: EnableCoinsBep2Provider,
        private val bep20Provider: EnableCoinsBep20Provider,
        private val coinManager: ICoinManager) {

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

        when (state.tokenType) {
            is TokenType.Erc20 -> fetchErc20Tokens(state.tokenType.words)
            is TokenType.Bep2 -> fetchBep2Tokens(state.tokenType.words)
            is TokenType.Bep20 -> fetchBep20Tokens(state.tokenType.words)
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
        try {
            val address = EthereumKit.address(words, EthereumKit.NetworkType.BscMainNet)

            state = State.Loading
            disposables.clear()

            bep20Provider.tokens(address.hex)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe({ coins ->
                        handleReceivedCoins(coins)
                    }, {
                        state = State.Failure(it)
                    })
                    .let { disposables.add(it) }

        } catch (err: Throwable) {
            state = State.Failure(err)
        }
    }

    private fun fetchErc20Tokens(words: List<String>) {
        try {
            val networkType = if (appConfigProvider.testMode) EthereumKit.NetworkType.EthRopsten else EthereumKit.NetworkType.EthMainNet

            val address = EthereumKit.address(words, networkType)

            state = State.Loading
            disposables.clear()
            ethereumProvider.tokens(address.hex)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe({ coins ->
                        handleReceivedCoins(coins)
                    }, {
                        state = State.Failure(it)
                    })
                    .let { disposables.add(it) }
        } catch (err: Throwable) {
            state = State.Failure(err)
        }
    }

    private fun fetchBep2Tokens(words: List<String>) {
        state = State.Loading

        try {
            disposables.clear()
            bep2Provider.getTokenSymbolsAsync(words)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe({ symbols ->
                        handleFetchBep2(symbols)
                    }, {
                        state = State.Failure(it)
                    })
                    .let { disposables.add(it) }
        } catch (err: Throwable) {
            state = State.Failure(err)
        }
    }

    private fun handleReceivedCoins(fetchedCoins: List<Coin>) {
        val listedTokens = mutableListOf<Coin>()
        val nonListedTokens = mutableListOf<Coin>()

        fetchedCoins.forEach { coin ->
            coinManager.getCoin(coin.type)?.let { listedCoin ->
                listedTokens.add(listedCoin)
            } ?: kotlin.run {
                nonListedTokens.add(coin)
            }
        }

        nonListedTokens.forEach {
            coinManager.save(it)
        }

        state = State.Success(listedTokens + nonListedTokens)
        enableCoinsAsync.onNext(listedTokens + nonListedTokens)
    }

    private fun handleFetchBep2(symbols: List<String>) {
        val coins = symbols.mapNotNull { symbol ->
            if (symbol == "BNB") {
                null
            } else {
                coinManager.coins.find {
                    it.type == CoinType.Bep2(symbol)
                }
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
