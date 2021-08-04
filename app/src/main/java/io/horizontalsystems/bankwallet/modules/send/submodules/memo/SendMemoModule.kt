package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.send.SendModule


object SendMemoModule {

    interface IView {
        fun setMaxLength(maxLength: Int)
        fun setHidden(hidden: Boolean)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onTextEntered(memo: String)
    }

    interface IMemoModule {
        val memo: String?
        fun setHidden(hidden: Boolean)
    }

    class Factory(
        private val maxLength: Int,
        private val hidden: Boolean,
        private val handler: SendModule.ISendHandler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendMemoView()
            val presenter = SendMemoPresenter(maxLength, hidden, view)

            handler.memoModule = presenter

            return presenter as T

        }
    }


}
