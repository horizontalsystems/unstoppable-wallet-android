package io.horizontalsystems.bankwallet.modules.sendevm

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ISendEthereumAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData.AdditionalInfo
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.IRecipientAddressService
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import io.horizontalsystems.ethereumkit.models.Address as EvmAddress

class SendEvmService(
        private val sendCoin: Coin,
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

    private val amountErrorSubject = PublishSubject.create<Optional<Throwable>>()
    private var amountError: Throwable? = null
        set(value) {
            field = value
            amountErrorSubject.onNext(Optional.ofNullable(value))
        }
    val amountErrorObservable: Flowable<Optional<Throwable>>
        get() = amountErrorSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val addressErrorSubject = PublishSubject.create<Optional<Throwable>>()
    private var addressError: Throwable? = null
        set(value) {
            field = value
            addressErrorSubject.onNext(Optional.ofNullable(value))
        }

    private fun syncState() {
        val amountError = this.amountError
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
            amount.movePointRight(sendCoin.decimal).toBigInteger()
        } catch (error: Throwable) {
            throw AmountError.InvalidDecimal
        }
        if (amount > adapter.balance) {
            throw AmountError.InsufficientBalance
        }
        return evmAmount
    }

    //region IAvailableBalanceService
    override val availableBalance: BigDecimal
        get() = adapter.balance
    //endregion

    //region IAmountInputService
    override val amount: BigDecimal
        get() = BigDecimal.ZERO

    override val coin: Coin
        get() = sendCoin

    override val balance: BigDecimal
        get() = adapter.balance

    override val amountObservable: Flowable<BigDecimal>
        get() = Flowable.empty()

    override val coinObservable: Flowable<Optional<Coin>>
        get() = Flowable.empty()

    override fun onChangeAmount(amount: BigDecimal) {
        if (amount > BigDecimal.ZERO) {
            try {
                evmAmount = validEvmAmount(amount)
                amountError = null
            } catch (error: Throwable) {
                evmAmount = null
                amountError = error
            }
        } else {
            evmAmount = null
            amountError = null
        }

        syncState()
    }
    //endregion

    //region IRecipientAddressService
    override val initialAddress: Address?
        get() = null

    override val error: Throwable?
        get() = addressError

    override val errorObservable: Observable<Unit>
        get() = addressErrorSubject.map { }

    override fun set(address: Address?) {
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

    override fun set(amount: BigDecimal) {
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
                return App.instance.localizedContext().getString(R.string.Swap_ErrorInsufficientBalance)
            }
        }
    }

    data class AddressData(val evmAddress: EvmAddress, val domain: String?)

}
