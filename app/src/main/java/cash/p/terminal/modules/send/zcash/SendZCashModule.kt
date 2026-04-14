package cash.p.terminal.modules.send.zcash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.core.managers.PendingTransactionRegistrar
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.getMaxSendableBalance
import org.koin.java.KoinJavaComponent.inject

object SendZCashModule {

    class Factory(
        private val wallet: Wallet,
        private val address: Address?,
        private val hideAddress: Boolean,
        private val adapter: ISendZcashAdapter
    ) : ViewModelProvider.Factory {
        private val adapterManager: IAdapterManager by inject(IAdapterManager::class.java)
        private val pendingRegistrar: PendingTransactionRegistrar by inject(PendingTransactionRegistrar::class.java)

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            val availableBalance = adapterManager.getMaxSendableBalance(wallet, adapter.maxSpendableBalance)
            val amountService = SendAmountService(
                amountValidator = AmountValidator(),
                coinCode = wallet.coin.code,
                availableBalance = availableBalance
            )
            val addressService = SendZCashAddressService(adapter)
            val memoService = SendZCashMemoService()

            return SendZCashViewModel(
                adapter = adapter,
                wallet = wallet,
                xRateService = xRateService,
                amountService = amountService,
                addressService = addressService,
                memoService = memoService,
                contactsRepo = App.contactsRepository,
                showAddressInput = !hideAddress,
                address = address,
                pendingRegistrar = pendingRegistrar,
                adapterManager = adapterManager
            ) as T
        }
    }
}
