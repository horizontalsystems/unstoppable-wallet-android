package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ICoinStorage
import io.horizontalsystems.bankwallet.core.managers.TokenSyncer
import io.horizontalsystems.bankwallet.entities.Coin
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ManageCoinsInteractor(private val coinManager: ICoinManager, private val coinStorage: ICoinStorage, private val tokenSyncer: TokenSyncer)
    : ManageCoinsModule.IInteractor {

    var delegate: ManageCoinsModule.IInteractorDelegate? = null
    private val disposables = CompositeDisposable()

    override fun syncCoins() {
        tokenSyncer.sync()
    }

    override fun loadCoins() {
        disposables.add(coinManager.allCoinsObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { allCoins ->
                    delegate?.didLoadAllCoins(allCoins)
                })

        disposables.add(coinStorage.enabledCoinsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { enabledCoins ->
                    delegate?.didLoadEnabledCoins(enabledCoins)
                })
    }

    override fun saveEnabledCoins(enabledCoins: List<Coin>) {
        coinStorage.save(enabledCoins)
        delegate?.didSaveChanges()
    }

}
