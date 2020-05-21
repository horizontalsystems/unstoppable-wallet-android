package io.horizontalsystems.bankwallet.modules.guides

import io.reactivex.disposables.Disposable

class GuidesInteractor() : GuidesModule.Interactor {

    var delegate: GuidesModule.InteractorDelegate? = null

    private var disposable: Disposable? = null


}
