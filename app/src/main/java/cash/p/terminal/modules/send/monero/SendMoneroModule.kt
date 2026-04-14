package cash.p.terminal.modules.send.monero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendMoneroAdapter
import cash.p.terminal.core.isNative
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.getMaxSendableBalance
import org.koin.java.KoinJavaComponent.inject
import java.math.RoundingMode

object SendMoneroModule {

    class Factory(
        private val wallet: Wallet,
        private val address: Address?,
        private val hideAddress: Boolean,
        private val adapter: ISendMoneroAdapter
    ) : ViewModelProvider.Factory {
        private val adapterManager: IAdapterManager by inject(IAdapterManager::class.java)

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendMoneroViewModel::class.java -> {
                    val amountValidator = AmountValidator()
                    val coinMaxAllowedDecimals = wallet.token.decimals

                    val availableBalance = adapterManager.getMaxSendableBalance(wallet, adapter.maxSpendableBalance)
                    val amountService = SendAmountService(
                        amountValidator,
                        wallet.token.coin.code,
                        availableBalance.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
                        wallet.token.type.isNative,
                    )
                    val addressService = SendMoneroAddressService()
                    val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

                    SendMoneroViewModel(
                        wallet = wallet,
                        sendToken = wallet.token,
                        adapter = adapter,
                        xRateService = xRateService,
                        amountService = amountService,
                        addressService = addressService,
                        coinMaxAllowedDecimals = coinMaxAllowedDecimals,
                        showAddressInput = !hideAddress,
                        contactsRepo = App.contactsRepository,
                        connectivityManager = App.connectivityManager,
                        address = address,
                        adapterManager = adapterManager,
                    ) as T
                }

                else -> throw IllegalArgumentException()
            }
        }
    }

}