package io.horizontalsystems.bankwallet.modules.restore.eos

import androidx.lifecycle.ViewModel

class RestoreEosViewModel : ViewModel(), RestoreEosModule.IView {

    lateinit var delegate: RestoreEosModule.IViewDelegate

}