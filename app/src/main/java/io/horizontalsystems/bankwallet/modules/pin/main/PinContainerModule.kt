package io.horizontalsystems.bankwallet.modules.pin.main

object PinContainerModule {

    interface IView {

    }

    interface IViewDelegate {
        fun onBackPressed()

    }

    interface IRouter{
        fun closeActivity()
        fun closeApplication()
    }

    fun init(view: PinContainerViewModel, router: IRouter, showCancelButton: Boolean) {
        val presenter = PinContainerPresenter(router, showCancelButton)

        view.delegate = presenter
        presenter.view = view
    }
}
