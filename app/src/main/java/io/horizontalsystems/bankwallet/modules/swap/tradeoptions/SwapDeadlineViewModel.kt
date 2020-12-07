package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import kotlin.math.floor

class SwapDeadlineViewModel(private val service: SwapTradeOptionsService) : ViewModel(), IVerifiedInputViewModel {

    override val inputFieldButtonItems: List<InputFieldButtonItem>
        get() {
            val bounds = service.recommendedDeadlineBounds
            val lowerMinutes = toMinutes(bounds.lower)
            val upperMinutes = toMinutes(bounds.upper)
            return listOf(
                    InputFieldButtonItem(App.instance.getString(R.string.SwapSettings_DeadlineMinute, lowerMinutes)) {
                        inputFieldValueLiveData.postValue(lowerMinutes)
                        setInputFieldValue(lowerMinutes)
                    },
                    InputFieldButtonItem(App.instance.getString(R.string.SwapSettings_DeadlineMinute, upperMinutes)) {
                        inputFieldValueLiveData.postValue(upperMinutes)
                        setInputFieldValue(upperMinutes)
                    },
            )
        }

    override val inputFieldPlaceholder = toMinutes(defaultTtl)
    override val inputFieldValueLiveData = MutableLiveData<String?>(null)
    override val inputFieldCautionLiveData = MutableLiveData<Caution?>(null)

    init {
        val state = service.state

        if (state is ISwapTradeOptionsService.State.Valid) {
            if (state.tradeOptions.ttl != defaultTtl) {
                inputFieldValueLiveData.postValue(toMinutes(state.tradeOptions.ttl))
            }
        }
    }

    override fun setInputFieldValue(text: String?) {
        service.deadline = text?.toLongOrNull()?.times(60) ?: defaultTtl
    }

    override fun inputFieldIsValid(text: String?): Boolean {
        return text.isNullOrBlank() || text.toLongOrNull() != null
    }

    private fun toMinutes(seconds: Long): String {
        return floor(seconds / 60.0).toLong().toString()
    }

    companion object {
        const val defaultTtl: Long = 20 * 60
    }
}


interface IVerifiedInputViewModel {
    val inputFieldMaximumNumberOfLines: Int get() = 1
    val inputFieldCanEdit: Boolean get() = true

    val inputFieldButtonItems: List<InputFieldButtonItem> get() = listOf()
    val inputFieldInitialValue: String? get() = null
    val inputFieldPlaceholder: String? get() = null

    val inputFieldValueLiveData: LiveData<String?>
    val inputFieldCautionLiveData: LiveData<Caution?>

    fun setInputFieldValue(text: String?) = Unit
    fun inputFieldIsValid(text: String?): Boolean = true
}

data class InputFieldButtonItem(val title: String, val onClick: () -> Unit)

data class Caution(val text: String, val type: Type) {
    enum class Type {
        Error, Warning
    }
}