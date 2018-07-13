package bitcoin.wallet.modules.receive

import android.arch.lifecycle.ViewModel

class ReceiveViewModel : ViewModel(), ReceiveModule.IView, ReceiveModule.IRouter {

    lateinit var delegate: ReceiveModule.IViewDelegate

    fun init() {
        ReceiveModule.init(this, this)
    }

}
