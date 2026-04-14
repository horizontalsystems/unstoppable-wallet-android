package cash.p.terminal.modules.send.stellar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendStellarAdapter
import cash.p.terminal.core.isNative
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import org.koin.java.KoinJavaComponent.inject

object SendStellarModule {
    class Factory(
        private val wallet: Wallet,
        private val address: Address?,
        private val hideAddress: Boolean,
        private val adapter: ISendStellarAdapter
    ) : ViewModelProvider.Factory {
        private val adapterManager: IAdapterManager by inject(IAdapterManager::class.java)

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val amountValidator = AmountValidator()
            val coinMaxAllowedDecimals = wallet.token.decimals

            val adjustedBalance = adapterManager.getAdjustedBalanceData(wallet)?.available
            val maxSendable = adapter.maxSpendableBalance
            // Use minOf to ensure we don't show more than maxSpendableBalance (which accounts for fee)
            val availableBalance = if (adjustedBalance != null) {
                minOf(adjustedBalance, maxSendable)
            } else {
                maxSendable
            }
            val amountService = SendAmountService(
                amountValidator = amountValidator,
                coinCode = wallet.coin.code,
                availableBalance = availableBalance,
                leaveSomeBalanceForFee = wallet.token.type.isNative
            )
            val addressService = SendStellarAddressService()
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            val feeToken = App.coinManager.getToken(
                TokenQuery(
                    BlockchainType.Stellar,
                    TokenType.Native
                )
            ) ?: throw IllegalArgumentException()

            return SendStellarViewModel(
                wallet,
                wallet.token,
                feeToken,
                adapter,
                coinMaxAllowedDecimals,
                xRateService,
                address,
                !hideAddress,
                amountService,
                addressService,
                App.contactsRepository,
                SendStellarMinimumAmountService(adapter),
                adapterManager
            ) as T
        }
    }
}