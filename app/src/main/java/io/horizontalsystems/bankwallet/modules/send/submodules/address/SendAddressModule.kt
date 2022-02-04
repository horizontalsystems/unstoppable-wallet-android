package io.horizontalsystems.bankwallet.modules.send.submodules.address

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import java.math.BigDecimal

object SendAddressModule {

    interface IAddressModule {
        var currentAddress: Address?

        @Throws
        fun validAddress(): Address
        fun validateAddress()
    }

    interface IAddressModuleDelegate {
        fun validate(address: String)

        fun onUpdateAddress()
        fun onUpdateAmount(amount: BigDecimal)
    }

    class Factory(
        private val sendHandler: SendModule.ISendHandler,
        private val addressModuleDelete: IAddressModuleDelegate,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val presenter = SendAddressPresenter(addressModuleDelete)
            val viewModel = RecipientAddressViewModel(presenter)

            sendHandler.addressModule = presenter

            return viewModel as T
        }
    }
}
