package bitcoin.wallet.modules.receive

import android.support.v4.app.FragmentActivity
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.viewHelpers.TextHelper

object ReceiveModule {

    interface IView {
        fun showAddress(coinAddress: String)
        fun showError(error: Int)
        fun showCopied()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onCopyClick()
        fun onShareClick()
    }

    interface IInteractor {
        fun getReceiveAddress(coinCode: String)
        fun copyToClipboard(coinAddress: String)
    }

    interface IInteractorDelegate {
        fun didReceiveAddress(coinAddress: String)
        fun didFailToReceiveAddress(exception: Exception)
        fun didCopyToClipboard()
    }

    interface IRouter {
        fun openShareView(coinAddress: String)
    }

    fun init(view: ReceiveViewModel, router: IRouter, coinCode: String) {
        val interactor = ReceiveInteractor(Factory.blockchainManager, TextHelper)
        val presenter = ReceivePresenter(interactor, router, coinCode)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, coin: Coin) {
        ReceiveFragment.show(activity, coin)
    }

}
