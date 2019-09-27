package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


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

    class Factory(private val maxLength: Int) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendMemoView()
            val presenter = SendMemoPresenter(maxLength, view)

            return presenter as T

        }
    }


}
