package io.horizontalsystems.bankwallet.modules.guides

import androidx.lifecycle.ViewModel

class GuidesPresenter(
        val view: GuidesModule.View,
        private val interactor: GuidesModule.Interactor)
    : GuidesModule.ViewDelegate, GuidesModule.InteractorDelegate, ViewModel() {

    //  ViewDelegate

    override fun onLoad() {
    }

    override fun onGuideClick(item: GuideItem) {
        view.open(item)
    }

    //  InteractorDelegate



    //  ViewModel


}
