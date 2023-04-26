package io.horizontalsystems.bankwallet.modules.swap.settings

import android.util.Range
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.getState
import io.horizontalsystems.bankwallet.modules.swap.settings.ui.InputButton
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import java.util.Optional
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

    var errorState by mutableStateOf<DataState.Error?>(null)
        private set

    override val inputButtons: List<InputButton>
        get() {
            val bounds = service.recommendedDeadlineBounds
            val lowerMinutes = toMinutes(bounds.lower)
            val upperMinutes = toMinutes(bounds.upper)

            return listOf(
                InputButton(
                    Translator.getString(R.string.SwapSettings_DeadlineMinute, lowerMinutes),
                    lowerMinutes
                ),
                InputButton(
                    Translator.getString(R.string.SwapSettings_DeadlineMinute, upperMinutes),
                    upperMinutes
                )
            )
        }

    override val inputFieldPlaceholder = toMinutes(service.defaultDeadline)

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
        errorState = getState(caution)
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

    override fun onCleared() {
        disposable.clear()
    }
}

interface IVerifiedInputViewModel {
    val inputButtons: List<InputButton> get() = listOf()

    val initialValue: String? get() = null
    val inputFieldPlaceholder: String? get() = null

    fun onChangeText(text: String?) = Unit
    fun isValid(text: String?): Boolean = true
}

data class Caution(val text: String, val type: Type) {
    enum class Type {
        Error, Warning
    }
}
