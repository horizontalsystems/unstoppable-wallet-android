package io.horizontalsystems.bankwallet.modules.send.submodules.memo


object SendMemoModule {

    interface IView {
        fun setMaxLength(maxLength: Int)
    }

    interface IViewDelegate {
        var view: IView

        fun onViewDidLoad()
        fun onTextEntered(memo: String)
    }

    interface IMemoModule {
        val memo: String?
    }

    fun init(view: SendMemoViewModel, maxLength: Int): IMemoModule {
        val presenter = SendMemoPresenter(maxLength)
        view.delegate = presenter

        return presenter
    }

}
