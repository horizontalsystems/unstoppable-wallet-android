package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel(), MainModule.IView, MainModule.IRouter {

    lateinit var delegate: MainModule.IViewDelegate

    fun init() {
        MainModule.init(this, this)
        delegate.viewDidLoad()
    }

}
