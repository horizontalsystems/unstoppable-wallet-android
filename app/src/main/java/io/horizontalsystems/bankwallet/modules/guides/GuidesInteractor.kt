package io.horizontalsystems.bankwallet.modules.guides

import io.horizontalsystems.bankwallet.core.managers.GuidesManager
import io.horizontalsystems.bankwallet.entities.Guide
import io.reactivex.disposables.Disposable

class GuidesInteractor(private val guidesManager: GuidesManager) : GuidesModule.Interactor {

    var delegate: GuidesModule.InteractorDelegate? = null

    override val guides: List<Guide>
        get() = guidesManager.guides

    private var disposable: Disposable? = null


}
