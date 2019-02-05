package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinStorage
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import org.web3j.tuples.generated.Tuple3

class TokenSyncer(private val networkManager: INetworkManager, private val coinStorage: ICoinStorage) {

    private val disposables = CompositeDisposable()

    fun sync() {
        Flowable.zip(networkManager.getTokens(), coinStorage.allCoinsObservable().take(1), coinStorage.enabledCoinsObservable().take(1),
                Function3 { new: List<Coin>, all: List<Coin>, enabled: List<Coin> ->
                    Tuple3(new, all, enabled)
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    update(it.value1, it.value2, it.value3)
                }, {
                    it.printStackTrace()
                })
                .let { disposables.add(it) }
    }

    private fun update(new: List<Coin>, all: List<Coin>, enabled: List<Coin>) {
        val newCoins = new.filter { !all.contains(it) }
        val delCoins = all.filter { !enabled.contains(it) && !new.contains(it) }
        if (delCoins.isEmpty() && newCoins.isEmpty()) {
            return
        }

        coinStorage.update(newCoins, delCoins)
    }
}
