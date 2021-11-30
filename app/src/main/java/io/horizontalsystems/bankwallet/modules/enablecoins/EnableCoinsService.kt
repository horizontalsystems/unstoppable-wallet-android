package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class EnableCoinsService(
    private val testMode: Boolean,
    private val bep2Provider: EnableCoinsBep2Provider,
    private val erc20Provider: EnableCoinsEip20Provider,
    private val bep20Provider: EnableCoinsEip20Provider
) {

    val enableCoinTypesAsync = PublishSubject.create<List<CoinType>>()
    val stateAsync = BehaviorSubject.createDefault<State>(State.Idle)

    var state: State = State.Idle
        private set(value) {
            field = value
            stateAsync.onNext(value)
        }

    private var disposables = CompositeDisposable()

    fun handle(coinTypes: List<CoinType>, accountType: AccountType) {
        val tokenTypes = coinTypes.mapNotNull { resolveTokenType(it, accountType) }

        if (tokenTypes.isNotEmpty()) {
            state = State.WaitingForApprove(tokenTypes.first())
        }
    }

    fun approveEnable() {
        val state = state
        if (state !is State.WaitingForApprove) {
            return
        }

        this.state = State.Loading
        disposables.clear()

        try {
            when (val tokenType = state.tokenType) {
                is TokenType.Erc20 -> fetchErc20Tokens(tokenType)
                is TokenType.Bep2 -> fetchBep2Tokens(tokenType)
                is TokenType.Bep20 -> fetchBep20Tokens(tokenType)
            }
        } catch (err: Throwable) {
            this.state = State.Failure(err)
        }
    }

    private fun resolveTokenType(coinType: CoinType, accountType: AccountType): TokenType? {
        return when {
            accountType !is AccountType.Mnemonic -> null
            coinType is CoinType.Ethereum -> TokenType.Erc20(accountType.words, accountType.passphrase)
            coinType is CoinType.BinanceSmartChain -> TokenType.Bep20(accountType.words, accountType.passphrase)
            coinType is CoinType.Bep2 && coinType.symbol == "BNB" -> TokenType.Bep2(
                accountType.words,
                accountType.passphrase
            )
            else -> null
        }
    }

    private fun fetchBep20Tokens(bep20: TokenType.Bep20) {
        val address = EthereumKit.address(bep20.words, bep20.passphrase, EthereumKit.NetworkType.BscMainNet)

        bep20Provider.getCoinTypesAsync(address.hex)
            .subscribeIO({ coinTypes ->
                state = State.Success(coinTypes)
                enableCoinTypesAsync.onNext(coinTypes)
            }, {
                state = State.Failure(it)
            })
            .let { disposables.add(it) }
    }

    private fun fetchErc20Tokens(erc20: TokenType.Erc20) {
        val networkType = if (testMode) EthereumKit.NetworkType.EthRopsten else EthereumKit.NetworkType.EthMainNet
        val address = EthereumKit.address(erc20.words, erc20.passphrase, networkType)

        erc20Provider.getCoinTypesAsync(address.hex)
            .subscribeIO({ coins ->
                state = State.Success(coins)
                enableCoinTypesAsync.onNext(coins)
            }, {
                state = State.Failure(it)
            })
            .let { disposables.add(it) }
    }

    private fun fetchBep2Tokens(bep2: TokenType.Bep2) {
        bep2Provider.getTokenSymbolsAsync(bep2.words, bep2.passphrase)
            .subscribeIO({ coins ->
                handleFetchBep2(coins)
            }, {
                state = State.Failure(it)
            })
            .let { disposables.add(it) }
    }

    private fun handleFetchBep2(tokenSymbols: List<String>) {
        val coinTypes = tokenSymbols.mapNotNull { tokenSymbol ->
            if (tokenSymbol == "BNB") {
                null
            } else {
                CoinType.Bep2(tokenSymbol)
            }
        }

        state = State.Success(coinTypes)
        enableCoinTypesAsync.onNext(coinTypes)
    }

    sealed class State {
        object Idle : State()
        class WaitingForApprove(val tokenType: TokenType) : State()
        object Loading : State()
        class Success(val coinTypes: List<CoinType>) : State()
        class Failure(val error: Throwable) : State()
    }

    sealed class TokenType {
        class Erc20(val words: List<String>, val passphrase: String) : TokenType()
        class Bep2(val words: List<String>, val passphrase: String) : TokenType()
        class Bep20(val words: List<String>, val passphrase: String) : TokenType()

        val title: String
            get() = when (this) {
                is Erc20 -> "ERC20"
                is Bep2 -> "BEP2"
                is Bep20 -> "BEP20"
            }
    }
}
