package io.horizontalsystems.bankwallet.modules.managecoins

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ICoinStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ManageCoinsInteractor(private val coinManager: ICoinManager, private val coinStorage: ICoinStorage)
    : ManageCoinsModule.IInteractor {

    var delegate: ManageCoinsModule.IInteractorDelegate? = null
    private val disposables = CompositeDisposable()

    override fun loadCoins() {
        delegate?.didLoadAllCoins(coinManager.allCoins)

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

    override fun clear() {
        disposables.clear()
    }

}
