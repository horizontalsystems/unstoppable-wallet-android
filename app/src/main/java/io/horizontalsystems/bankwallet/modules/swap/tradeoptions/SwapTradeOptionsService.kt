package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import android.util.Range
import io.horizontalsystems.bankwallet.core.managers.UnstoppableDomainsService
import io.horizontalsystems.bankwallet.modules.swap.SwapTradeService
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.ISwapTradeOptionsService.*
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.ISwapTradeOptionsService.TradeOptionsError.*
import io.horizontalsystems.ethereumkit.core.AddressValidator
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal

class SwapTradeOptionsService(private val tradeService: SwapTradeService, private val uDomainService: UnstoppableDomainsService)
    : ISwapTradeOptionsService {

    private var resolveDisposable: Disposable? = null

    val tradeOptions = tradeService.tradeOptions

    val recipient = Field(tradeService.tradeRecipientDomain ?: tradeOptions.recipient?.hex, onValueChange = {
        resolveDisposable?.dispose()
        resolveDisposable = null
    })

    val slippage = Field(tradeOptions.allowedSlippagePercent, onValueChange = {
        validateSlippage()
    })

    val deadline = Field(tradeOptions.ttl, onValueChange = {
        validateDeadline()
    })

    fun validateSlippage() {
        slippage.state = FieldState.Validating

        if (slippage.value.compareTo(BigDecimal.ZERO) == 0) {
            slippage.state = FieldState.NotValid(ZeroSlippage)
        } else if (slippage.value > limitSlippageBounds.upper) {
            slippage.state = FieldState.NotValid(InvalidSlippage(InvalidSlippageType.Higher(limitSlippageBounds.upper)))
        } else if (slippage.value < limitSlippageBounds.lower) {
            slippage.state = FieldState.NotValid(InvalidSlippage(InvalidSlippageType.Lower(limitSlippageBounds.lower)))
        } else {
            tradeOptions.allowedSlippagePercent = slippage.value
            slippage.state = FieldState.Valid
        }
    }

    fun validateDeadline() {
        deadline.state = FieldState.Validating

        if (deadline.value == 0L) {
            deadline.state = FieldState.NotValid(ZeroDeadline)
        } else {
            tradeOptions.ttl = deadline.value
            deadline.state = FieldState.Valid
        }
    }

    fun validateRecipient() {
        val address = recipient.value?.trim()
        if (address.isNullOrBlank()) {
            tradeOptions.recipient = null
            tradeService.tradeRecipientDomain = null
            recipient.state = FieldState.Valid
        } else {
            validateDomain(address)
        }
    }

    fun apply(): Single<Boolean> {
        val needToValidate = mutableListOf<BehaviorSubject<FieldState>>()

        if (recipient.state is FieldState.NotValidated) {
            needToValidate.add(recipient.stateObservable)
            validateRecipient()
        }

        if (slippage.state is FieldState.NotValidated) {
            needToValidate.add(slippage.stateObservable)
            validateSlippage()
        }

        if (deadline.state is FieldState.NotValidated) {
            needToValidate.add(deadline.stateObservable)
            validateDeadline()
        }

        if (needToValidate.isEmpty()) {
            return Single.just(true)
        }

        return Observable.combineLatest(needToValidate) { it }
                .filter {
                    listOf(recipient.state, slippage.state, deadline.state).all {
                        it !is FieldState.Validating
                    }
                }
                .flatMap {
                    Observable.just(listOf(recipient.state, slippage.state, deadline.state).all { it is FieldState.Valid })
                }
                .first(false)
    }

    private fun validateDomain(domain: String) {
        when (recipient.state) {
            is FieldState.NotValid,
            is FieldState.Valid -> {
                recipient.stateObservable.onNext(recipient.state)
                return
            }
        }

        recipient.state = FieldState.Validating
        resolveDisposable?.dispose()

        val ticker = tradeService.coinTo?.code
        if (ticker == null) {
            validateAddress(domain)
            return
        }

        uDomainService.resolveDomain(domain, ticker)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ address ->
                    tradeService.tradeRecipientDomain = domain
                    validateAddress(address)
                }, {
                    tradeService.tradeRecipientDomain = null
                    validateAddress(domain)
                }).let {
                    resolveDisposable = it
                }
    }

    private fun validateAddress(address: String) {
        try {
            tradeOptions.recipient = Address(address)
            recipient.value = address
            recipient.state = FieldState.Valid
        } catch (e: NumberFormatException) {
            recipient.state = FieldState.NotValid(InvalidAddress)
        } catch (e: AddressValidator.AddressValidationException) {
            recipient.state = FieldState.NotValid(InvalidAddress)
        }
    }

    companion object {
        val recommendedSlippageBounds = Range(BigDecimal("0.1"), BigDecimal("1"))
        val recommendedDeadlineBounds = Range(600L, 1800L)
        val limitSlippageBounds = Range(BigDecimal("0.01"), BigDecimal("20"))
    }
}
