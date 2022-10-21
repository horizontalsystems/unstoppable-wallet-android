package io.horizontalsystems.bankwallet.modules.coin

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class CoinService(
    val fullCoin: FullCoin,
    private val marketFavoritesManager: MarketFavoritesManager,
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager,
) : Clearable {

    private val _isFavorite = BehaviorSubject.create<Boolean>()
    val isFavorite: Observable<Boolean>
        get() = _isFavorite

    private val _coinState = BehaviorSubject.create<CoinState>()
    val coinState: Observable<CoinState>
        get() = _coinState

    private val initialCoinInWallet = walletManager.activeWallets.any { it.coin.uid == fullCoin.coin.uid }
    private val disposables = CompositeDisposable()

    init {
        emitCoinState()
        emitIsFavorite()

        walletManager.activeWalletsUpdatedObservable
            .subscribeIO {
                emitCoinState()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun emitCoinState() {
        val activeAccount = accountManager.activeAccount
        _coinState.onNext(when {
            activeAccount == null -> CoinState.NoActiveAccount
            activeAccount.isWatchAccount -> CoinState.WatchAccount
            fullCoin.eligibleTokens(activeAccount.type).isEmpty() -> CoinState.Unsupported
            walletManager.activeWallets.any { it.coin.uid == fullCoin.coin.uid } -> {
                if (initialCoinInWallet) {
                    CoinState.InWallet
                } else {
                    CoinState.AddedToWallet
                }
            }
            else -> CoinState.NotInWallet
        })
    }

    override fun clear() {
        disposables.clear()
    }

    fun favorite() {
        marketFavoritesManager.add(fullCoin.coin.uid)

        emitIsFavorite()
    }

    fun unfavorite() {
        marketFavoritesManager.remove(fullCoin.coin.uid)

        emitIsFavorite()
    }

    private fun emitIsFavorite() {
        _isFavorite.onNext(marketFavoritesManager.isCoinInFavorites(fullCoin.coin.uid))
    }
}
