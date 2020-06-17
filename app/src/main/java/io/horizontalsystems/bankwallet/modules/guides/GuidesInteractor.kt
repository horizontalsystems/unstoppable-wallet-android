package io.horizontalsystems.bankwallet.modules.guides

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class GuidesInteractor(private val guidesManager: GuidesManager, private val networkManager: INetworkManager) : GuidesModule.Interactor {

    var delegate: GuidesModule.InteractorDelegate? = null

    private var disposable: Disposable? = null

    override fun fetchGuideCategories() {
        disposable = guidesManager.getGuideCategories()
                .subscribeOn(Schedulers.io())
                .subscribe { categories, _ ->
                    delegate?.didFetchGuideCategories(categories)
                }
    }

}
