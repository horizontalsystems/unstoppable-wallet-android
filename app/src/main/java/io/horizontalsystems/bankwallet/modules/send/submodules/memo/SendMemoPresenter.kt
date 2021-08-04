package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import androidx.lifecycle.ViewModel

class SendMemoPresenter(private val maxLength: Int,
                        private val hidden: Boolean,
                        var view: SendMemoModule.IView)
    : ViewModel(), SendMemoModule.IViewDelegate, SendMemoModule.IMemoModule {

    // SendMemoModule.IMemoModule

    override var memo: String? = null
        private set

    override fun setHidden(hidden: Boolean) {
        view.setHidden(hidden)
    }

    // SendMemoModule.IViewDelegate

    override fun onViewDidLoad() {
        view.setMaxLength(maxLength)
        view.setHidden(hidden)
    }

    override fun onTextEntered(memo: String) {
        this.memo = memo
    }

}
