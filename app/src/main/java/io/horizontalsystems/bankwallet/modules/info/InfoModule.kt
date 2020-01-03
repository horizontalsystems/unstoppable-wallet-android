package io.horizontalsystems.bankwallet.modules.info

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import kotlinx.android.parcel.Parcelize

object InfoModule {
    interface IView {
        fun set(title: String, description: String)
        fun setTxHash(txHash: String)
        fun setConflictingTxHash(conflictingTxHash: String)
        fun showCopied()
    }

    interface IViewDelegate {
        fun onLoad(infoParameters: InfoParameters)
        fun onClickClose()
        fun onClickTxHash(txHash: String)
    }

    interface IInteractor {
        fun onCopy(value: String)
    }

    interface IRouter {
        fun goBack()
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = InfoView()
            val router = InfoRouter()
            val interactor = InfoInteractor(TextHelper)
            val presenter = InfoPresenter(interactor, view, router)

            return presenter as T
        }
    }

    const val KEY_INFO_PARAMETERS = "info_parameters"

    @Parcelize
    data class InfoParameters(val title: String,
                              val description: String,
                              val txHash: String? = null,
                              val conflictingTxHash: String? = null) : Parcelable

    fun start(context: Context, infoParameters: InfoParameters) {
        val intent = Intent(context, InfoActivity::class.java)
        intent.putParcelableExtra(KEY_INFO_PARAMETERS, infoParameters)
        context.startActivity(intent)
    }

}
