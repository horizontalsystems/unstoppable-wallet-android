package io.horizontalsystems.bankwallet.modules.sendevm

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData.AdditionalInfo
import io.horizontalsystems.bankwallet.modules.swap.settings.IRecipientAddressService
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import io.horizontalsystems.ethereumkit.models.Address as EvmAddress

class SendEvmService(
    val sendCoin: PlatformCoin,
    val adapter: ISendEthereumAdapter
) : IAvailableBalanceService, IAmountInputService, IRecipientAddressService, Clearable {

    private val stateSubject = PublishSubject.create<State>()
    var state: State = State.NotReady
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    val stateObservable: Flowable<State>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    private var evmAmount: BigInteger? = null
    private var addressData: AddressData? = null

    private val amountCautionSubject = PublishSubject.create<AmountCaution>()
    private var amountCaution: AmountCaution = AmountCaution()
        set(value) {
            field = value
            amountCautionSubject.onNext(value)
        }
    val amountCautionObservable: Flowable<AmountCaution>
        get() = amountCautionSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val addressErrorSubject = PublishSubject.create<Optional<Throwable>>()
    private var addressError: Throwable? = null
        set(value) {
            field = value
            addressErrorSubject.onNext(Optional.ofNullable(value))
        }

    private fun syncState() {
        val amountError = this.amountCaution.error
        val addressError = this.addressError
        val evmAmount = this.evmAmount
        val addressData = this.addressData

        state = if (amountError == null && addressError == null && evmAmount != null && addressData != null) {
            val transactionData = adapter.getTransactionData(evmAmount, addressData.evmAddress)
            val additionalInfo = AdditionalInfo.Send(SendEvmData.SendInfo(addressData.domain))
            State.Ready(SendEvmData(transactionData, additionalInfo))
        } else {
            State.NotReady
        }
    }

    @Throws
    private fun validEvmAmount(amount: BigDecimal): BigInteger {
        val evmAmount = try {
            amount.movePointRight(sendCoin.decimals).toBigInteger()
        } catch (error: Throwable) {
            throw AmountError.InvalidDecimal
        }
        if (amount > adapter.balanceData.available) {
            throw AmountError.InsufficientBalance
        }
        return evmAmount
    }

    //region IAvailableBalanceService
    override val availableBalance: BigDecimal
        get() = adapter.balanceData.available
    //endregion

    //region IAmountInputService
    override val amount: BigDecimal
        get() = BigDecimal.ZERO

    override val coin: PlatformCoin
        get() = sendCoin

    override val balance: BigDecimal
        get() = adapter.balanceData.available

    override val amountObservable: Flowable<BigDecimal>
        get() = Flowable.empty()

    override val coinObservable: Flowable<Optional<PlatformCoin>>
        get() = Flowable.empty()

    override fun onChangeAmount(amount: BigDecimal) {
        if (amount > BigDecimal.ZERO) {
            var amountWarning: AmountWarning? = null
            try {
                if (amount == balance && (sendCoin.coinType is CoinType.Ethereum || sendCoin.coinType is CoinType.BinanceSmartChain)) {
                    amountWarning = AmountWarning.CoinNeededForFee
                }
                evmAmount = validEvmAmount(amount)
                amountCaution = AmountCaution(null, amountWarning)
            } catch (error: Throwable) {
                evmAmount = null
                amountCaution = AmountCaution(error, null)
            }
        } else {
            evmAmount = null
            amountCaution = AmountCaution()
        }

        syncState()
    }
    //endregion

    //region IRecipientAddressService
    override val initialAddress: Address?
        get() = addressData?.let{ Address(it.evmAddress.hex, it.domain) }

    override val recipientAddressError: Throwable?
        get() = addressError

    override val recipientAddressErrorObservable: Observable<Unit>
        get() = addressErrorSubject.map { }

    override fun setRecipientAddress(address: Address?) {
        if (address != null && address.hex.isNotEmpty()) {
            try {
                AddressValidator.validate(address.hex)
                addressData = AddressData(evmAddress = EvmAddress(address.hex), domain = address.domain)
                addressError = null
            } catch (error: Throwable) {
                addressData = null
                addressError = error
            }
        } else {
            addressData = null
            addressError = null
        }
        syncState()
    }

    override fun setRecipientAmount(amount: BigDecimal) {
        //TODO
    }

    override fun clear() = Unit

    //endregion

    sealed class State {
        class Ready(val sendData: SendEvmData) : State()
        object NotReady : State()
    }

    sealed class AmountError : Throwable() {
        object InvalidDecimal : AmountError()
        object InsufficientBalance : AmountError() {
            override fun getLocalizedMessage(): String {
                return Translator.getString(R.string.Swap_ErrorInsufficientBalance)
            }
        }
    }

    class AmountCaution(val error: Throwable? = null, val amountWarning: AmountWarning? = null)

    enum class AmountWarning {
        CoinNeededForFee
    }

    data class AddressData(val evmAddress: EvmAddress, val domain: String?)

}
