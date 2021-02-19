package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.core.SingleLiveEvent
import kotlin.math.floor

class SwapDeadlineViewModel(
        private val service: SwapTradeOptionsService,
        private val stringProvider: StringProvider
) : ViewModel(), IVerifiedInputViewModel {

    override val inputFieldButtonItems: List<InputFieldButtonItem>
        get() {
            val bounds = SwapTradeOptionsService.recommendedDeadlineBounds
            val lowerMinutes = toMinutes(bounds.lower)
            val upperMinutes = toMinutes(bounds.upper)

            return listOf(
                    InputFieldButtonItem(stringProvider.string(R.string.SwapSettings_DeadlineMinute, lowerMinutes)) {
                        setTextLiveData.postValue(lowerMinutes)
                        onChangeText(lowerMinutes)
                    },
                    InputFieldButtonItem(stringProvider.string(R.string.SwapSettings_DeadlineMinute, upperMinutes)) {
                        setTextLiveData.postValue(upperMinutes)
                        onChangeText(upperMinutes)
                    },
            )
        }

    override val inputFieldPlaceholder = toMinutes(defaultTtl)
    override val setTextLiveData = MutableLiveData<String?>(null)
    override val cautionLiveData = MutableLiveData<Caution?>(null)
    override val initialValue: String?
        get() {
            val state = service.state

            if (state is ISwapTradeOptionsService.State.Valid) {
                if (state.tradeOptions.ttl != defaultTtl) {
                    return toMinutes(state.tradeOptions.ttl)
                }
            }

            return null
        }

    override fun onChangeText(text: String?) {
        service.deadline = text?.toLongOrNull()?.times(60) ?: defaultTtl
    }

    override fun isValid(text: String?): Boolean {
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
    val initialValue: String? get() = null
    val inputFieldPlaceholder: String? get() = null

    val setTextLiveData: LiveData<String?>
    val cautionLiveData: LiveData<Caution?>
    val isLoadingLiveData: LiveData<Boolean> get() = SingleLiveEvent<Boolean>()

    fun onChangeText(text: String?) = Unit
    fun isValid(text: String?): Boolean = true
}

data class InputFieldButtonItem(val title: String, val onClick: () -> Unit)

data class Caution(val text: String, val type: Type) {
    enum class Type {
        Error, Warning
    }
}