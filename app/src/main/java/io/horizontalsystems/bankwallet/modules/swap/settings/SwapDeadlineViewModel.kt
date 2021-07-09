package io.horizontalsystems.bankwallet.modules.swap.settings

import android.util.Range
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import kotlin.math.floor

interface ISwapDeadlineService {
    val initialDeadline: Long?
    val defaultDeadline: Long

    val deadlineError: Throwable?
    val deadlineErrorObservable: Observable<Optional<Throwable>>

    val recommendedDeadlineBounds: Range<Long>

    fun setDeadline(value: Long)
}

class SwapDeadlineViewModel(
        private val service: ISwapDeadlineService
) : ViewModel(), IVerifiedInputViewModel {

    private val disposable = CompositeDisposable()

    override val inputFieldButtonItems: List<InputFieldButtonItem>
        get() {
            val bounds = service.recommendedDeadlineBounds
            val lowerMinutes = toMinutes(bounds.lower)
            val upperMinutes = toMinutes(bounds.upper)

            return listOf(
                    InputFieldButtonItem(Translator.getString(R.string.SwapSettings_DeadlineMinute, lowerMinutes)) {
                        setTextLiveData.postValue(lowerMinutes)
                        onChangeText(lowerMinutes)
                    },
                    InputFieldButtonItem(Translator.getString(R.string.SwapSettings_DeadlineMinute, upperMinutes)) {
                        setTextLiveData.postValue(upperMinutes)
                        onChangeText(upperMinutes)
                    },
            )
        }

    override val inputFieldPlaceholder = toMinutes(service.defaultDeadline)
    override val setTextLiveData = MutableLiveData<String?>()
    override val cautionLiveData = MutableLiveData<Caution?>(null)
    override val initialValue: String?
        get() = service.initialDeadline?.let { toMinutes(it) }

    init {
        service.deadlineErrorObservable
                .subscribe { sync() }
                .let {
                    disposable.add(it)
                }
        sync()
    }

    private fun sync() {
        val caution = service.deadlineError?.localizedMessage?.let { localizedMessage ->
            Caution(localizedMessage, Caution.Type.Error)
        }
        cautionLiveData.postValue(caution)
    }

    override fun onChangeText(text: String?) {
        service.setDeadline(text?.toLongOrNull()?.times(60) ?: service.defaultDeadline)
    }

    override fun isValid(text: String?): Boolean {
        return text.isNullOrBlank() || text.toLongOrNull() != null
    }

    private fun toMinutes(seconds: Long): String {
        return floor(seconds / 60.0).toLong().toString()
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