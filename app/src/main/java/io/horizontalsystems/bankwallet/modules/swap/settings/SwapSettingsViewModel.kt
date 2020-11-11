package io.horizontalsystems.bankwallet.modules.swap.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettings
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettingsError.*
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettingsState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SwapSettingsViewModel(
        private val swapSettingsService: SwapSettingsModule.ISwapSettingsService,
        private val stringProvider: StringProvider
) : ViewModel() {

    private val disposables = CompositeDisposable()

    private val _enableApply = MutableLiveData<SwapSettings?>()
    val enableApply: LiveData<SwapSettings?> = _enableApply

    private val _slippageButtonTitles = MutableLiveData<Pair<String, String>>()
    val slippageButtonTitles: LiveData<Pair<String, String>> = _slippageButtonTitles

    private val _slippage = MutableLiveData<String?>()
    val slippage: LiveData<String?> = _slippage

    private val _slippageHint = MutableLiveData<String?>()
    val slippageHint: LiveData<String?> = _slippageHint

    private val _slippageError = MutableLiveData<String?>()
    val slippageError: LiveData<String?> = _slippageError

    private val _deadlineButtonTitles = MutableLiveData<Pair<String, String>>()
    val deadlineButtonTitles = _deadlineButtonTitles

    private val _deadline = MutableLiveData<String?>()
    val deadline: LiveData<String?> = _deadline

    private val _deadlineHint = MutableLiveData<String?>()
    val deadlineHint: LiveData<String?> = _deadlineHint

    private val _deadlineError = MutableLiveData<String?>()
    val deadlineError: LiveData<String?> = _deadlineError

    private val _recipientAddress = MutableLiveData<String?>()
    val recipientAddress: LiveData<String?> = _recipientAddress

    private val _recipientAddressHint = MutableLiveData<String?>()
    val recipientAddressHint: LiveData<String?> = _recipientAddressHint

    private val _recipientAddressError = MutableLiveData<String?>()
    val recipientAddressError: LiveData<String?> = _recipientAddressError

    init {
        if (swapSettingsService.slippage?.stripTrailingZeros() == swapSettingsService.defaultSlippage.stripTrailingZeros()) {
            _slippageHint.postValue(swapSettingsService.defaultSlippage.stripTrailingZeros()?.toPlainString())
        } else {
            _slippage.postValue(swapSettingsService.slippage?.stripTrailingZeros()?.toPlainString())
        }

        swapSettingsService.recommendedSlippageRange.apply {
            val leftButtonTitle = stringProvider.string(R.string.Swap_Percent, start.toString())
            val rightButtonTitle = stringProvider.string(R.string.Swap_Percent, endInclusive.toString())
            _slippageButtonTitles.postValue(Pair(leftButtonTitle, rightButtonTitle))
        }

        if (swapSettingsService.deadline == swapSettingsService.defaultDeadline) {
            _deadlineHint.postValue(swapSettingsService.defaultDeadline.toString())
        } else {
            _deadline.postValue(swapSettingsService.deadline.toString())
        }

        if (swapSettingsService.recipientAddress == null) {
            _recipientAddressHint.postValue(stringProvider.string(R.string.SwapSettings_RecipientAddressHint))
        } else {
            _recipientAddress.postValue(swapSettingsService.recipientAddress)
        }

        swapSettingsService.recommendedDeadlineRange.apply {
            val leftButtonTitle = stringProvider.string(R.string.Duration_Minutes, start)
            val rightButtonTitle = stringProvider.string(R.string.Duration_Minutes, endInclusive)
            _deadlineButtonTitles.postValue(Pair(leftButtonTitle, rightButtonTitle))
        }

        swapSettingsService.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe { state ->
                    _enableApply.postValue((state as? SwapSettingsState.Valid)?.settings)

                    updateSlippageError(state)
                    updateDeadlineError(state)
                    updateRecipientAddressError(state)
                }
                .let { disposables.add(it) }
    }

    fun setSlippage(slippage: String?) {
        swapSettingsService.enterSlippage(slippage?.toBigDecimalOrNull())
    }

    fun onSlippageLeftButtonClick() {
        val recommendedMinSlippage = swapSettingsService.recommendedSlippageRange.start
        _slippage.postValue(recommendedMinSlippage.toString())
        swapSettingsService.enterSlippage(recommendedMinSlippage)
    }

    fun onSlippageRightButtonClick() {
        val recommendedMaxSlippage = swapSettingsService.recommendedSlippageRange.endInclusive
        _slippage.postValue(recommendedMaxSlippage.toString())
        swapSettingsService.enterSlippage(recommendedMaxSlippage)
    }

    fun setDeadline(deadline: String?) {
        swapSettingsService.enterDeadline(deadline?.toLongOrNull())
    }

    fun onDeadlineLeftButtonClick() {
        val recommendedMinDeadline = swapSettingsService.recommendedDeadlineRange.start
        _deadline.postValue(recommendedMinDeadline.toString())
        swapSettingsService.enterDeadline(recommendedMinDeadline)

    }

    fun onDeadlineRightButtonClick() {
        val recommendedMaxDeadline = swapSettingsService.recommendedDeadlineRange.endInclusive
        _deadline.postValue(recommendedMaxDeadline.toString())
        swapSettingsService.enterDeadline(recommendedMaxDeadline)
    }

    fun setRecipientAddress(address: String?) {
        swapSettingsService.enterRecipientAddress(address)
    }

    private fun updateSlippageError(state: SwapSettingsState) {
        val slippageError = (state as? SwapSettingsState.Invalid)?.errors?.firstOrNull { it is SlippageError } as? SlippageError
        val errorText = slippageError?.let {
            when (it) {
                is SlippageError.SlippageTooHigh -> stringProvider.string(R.string.SwapSettings_Error_SlippageTooHigh, it.maxValue.stripTrailingZeros().toPlainString())
                is SlippageError.SlippageTooLow -> stringProvider.string(R.string.SwapSettings_Error_SlippageTooLow)
            }
        }
        _slippageError.postValue(errorText)
    }

    private fun updateDeadlineError(state: SwapSettingsState) {
        val deadlineError = (state as? SwapSettingsState.Invalid)?.errors?.firstOrNull { it is DeadlineError } as? DeadlineError
        val errorText = (deadlineError as? DeadlineError.DeadlineTooLow)?.let {
            stringProvider.string(R.string.SwapSettings_Error_DeadlineTooLow, deadlineError.minValue)
        }
        _deadlineError.postValue(errorText)
    }

    private fun updateRecipientAddressError(state: SwapSettingsState) {
        val addressError = (state as? SwapSettingsState.Invalid)?.errors?.firstOrNull { it is AddressError } as? AddressError
        val errorText = (addressError as? AddressError.InvalidAddress)?.let {
            stringProvider.string(R.string.SwapSettings_Error_InvalidAddress)
        }
        _recipientAddressError.postValue(errorText)
    }

}
