package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import androidx.lifecycle.ViewModel

class SendMemoPresenter(private val maxLength: Int,
                        var view: SendMemoModule.View)
    : ViewModel(), SendMemoModule.ViewDelegate, SendMemoModule.MemoModule {

    // SendMemoModule.IMemoModule

    override var memo: String? = null
        private set

    // SendMemoModule.IViewDelegate

    override fun onViewDidLoad() {
        view.setMaxLength(maxLength)
    }

    override fun onTextEntered(memo: String) {
        this.memo = memo
    }

}
