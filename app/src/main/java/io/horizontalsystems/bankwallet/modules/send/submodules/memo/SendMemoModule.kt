package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


object SendMemoModule {

    interface View {
        fun setMaxLength(maxLength: Int)
    }

    interface ViewDelegate {
        fun onViewDidLoad()
        fun onTextEntered(memo: String)
    }

    interface MemoModule {
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
