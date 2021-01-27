package io.horizontalsystems.bankwallet.modules.enablecoins

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.core.IBuildConfigProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class EnableCoinsService(
        private val appConfigProvider: IBuildConfigProvider,
        private val ethereumProvider: EnableCoinsErc20Provider,
        private val coinManager: ICoinManager) {

    val enableCoinsAsync = PublishSubject.create<List<Coin>>()
    val stateAsync = BehaviorSubject.createDefault<State>(State.Idle())

    var state: State = State.Idle()
        private set(value) {
            field = value
            stateAsync.onNext(value)
        }

    private var disposable: Disposable? = null

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
            is TokenType.Erc20 -> {
                fetchErc20Tokens(state.tokenType.words)
            }
            is TokenType.Bep2 -> {
            }
        }
    }

    private fun resolveTokenType(coinType: CoinType, accountType: AccountType): TokenType? {
        if (coinType is CoinType.Ethereum && accountType is AccountType.Mnemonic) {
            if (accountType.words.size == 12) {
                return TokenType.Erc20(accountType.words)
            }
        }

        if (coinType is CoinType.Binance && accountType is AccountType.Mnemonic) {
            if (coinType.symbol == "BNB" && accountType.words.size == 24) {
                return TokenType.Erc20(accountType.words)
            }
        }

        return null
    }

    private fun fetchErc20Tokens(words: List<String>) {
        try {
            val networkType = if (appConfigProvider.testMode) {
                EthereumKit.NetworkType.Ropsten
            } else {
                EthereumKit.NetworkType.MainNet
            }

            val address = EthereumKit.address(words, networkType)

            state = State.Loading()
            disposable?.dispose()
            disposable = ethereumProvider.contractAddressesSingle(address.hex)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe({ addresses ->
                        handleFetchErc20(addresses)
                    }, {
                        state = State.Failure(it)
                    })
        } catch (err: Throwable) {
            state = State.Failure(err)
        }
    }

    private fun handleFetchErc20(addresses: List<String>) {
        val coins = addresses.mapNotNull { address ->
            coinManager.coins.find { it.type == CoinType.Erc20(address) }
        }

        state = State.Success(coins)
        enableCoinsAsync.onNext(coins)
    }

    sealed class State {
        class Idle : State()
        class WaitingForApprove(val tokenType: TokenType) : State()
        class Loading : State()
        class Success(val coins: List<Coin>) : State()
        class Failure(val error: Throwable) : State()
    }

    sealed class TokenType {
        class Erc20(val words: List<String>) : TokenType()
        class Bep2(val words: List<String>) : TokenType()

        val title: String
            get() = when (this) {
                is Erc20 -> "ERC20"
                is Bep2 -> "BEP2"
            }
    }
}
