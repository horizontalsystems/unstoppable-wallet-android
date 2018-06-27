package bitcoin.wallet.modules.pay

import android.support.v4.app.FragmentActivity
import bitcoin.wallet.entities.coins.Coin

object PayModule {

    interface IView

    interface IViewDelegate

    interface IInteractor

    interface IInteractorDelegate

    interface IRouter

    fun init(view: IView, router: IRouter) {

    }

    fun start(activity: FragmentActivity, coin: Coin) {
        PayFragment.show(activity, coin)
    }

}
