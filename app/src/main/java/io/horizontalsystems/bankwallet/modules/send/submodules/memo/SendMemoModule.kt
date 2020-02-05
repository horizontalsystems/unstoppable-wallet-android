package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.send.SendModule


object SendMemoModule {

    interface IView {
        fun setMaxLength(maxLength: Int)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onTextEntered(memo: String)
    }

    interface IMemoModule {
        val memo: String?
    }

    class Factory(private val maxLength: Int, private val handler: SendModule.ISendHandler) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendMemoView()
            val presenter = SendMemoPresenter(maxLength, view)

            handler.memoModule = presenter

            return presenter as T

        }
    }


}
