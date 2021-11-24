package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.hodler.LockTimeInterval

object SendConfirmationModule {

    class ViewData(
        val coinName: String,
        val coinAmount: String,
        val currencyAmount: String?,
        val toAddress: String,
        val domain: String?,
        val memo: String?,
        val lockTimeInterval: LockTimeInterval?,
        val feeAmount: String,
    )

    class SendButton(@StringRes val title: Int, val enabled: Boolean)

    class Factory(private val confirmationViewItems: List<SendModule.SendConfirmationViewItem>?) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendConfirmationViewModel(confirmationViewItems) as T
        }
    }

}
