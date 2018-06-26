package bitcoin.wallet.modules.pay

import android.arch.lifecycle.ViewModel

class PayViewModel : ViewModel(), PayModule.IView, PayModule.IRouter {

    lateinit var delegate: PayModule.IViewDelegate

    fun init() {
        PayModule.init(this, this)
    }

}
