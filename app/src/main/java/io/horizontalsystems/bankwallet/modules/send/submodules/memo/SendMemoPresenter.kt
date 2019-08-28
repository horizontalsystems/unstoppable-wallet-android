package io.horizontalsystems.bankwallet.modules.send.submodules.memo

class SendMemoPresenter(private val maxLength: Int) : SendMemoModule.IViewDelegate, SendMemoModule.IMemoModule {

    // SendMemoModule.IMemoModule

    override var memo: String? = null
        private set

    // SendMemoModule.IViewDelegate

    override lateinit var view: SendMemoModule.IView

    override fun onViewDidLoad() {
        view.setMaxLength(maxLength)
    }

    override fun onTextEntered(memo: String) {
        this.memo = memo
    }

}
