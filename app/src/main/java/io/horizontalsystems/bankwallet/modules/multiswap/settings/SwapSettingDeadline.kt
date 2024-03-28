package io.horizontalsystems.bankwallet.modules.multiswap.settings

import android.util.Range
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.modules.swap.settings.ISwapDeadlineService
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapDeadlineViewModel
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule
import io.horizontalsystems.bankwallet.modules.swap.settings.ui.TransactionDeadlineInput
import io.reactivex.subjects.PublishSubject
import java.util.Optional

data class SwapSettingDeadline(
    val settings: Map<String, Any?>,
    val defaultTtl: Long,
) : ISwapSetting {
    override val id = "deadline"

    val value = settings[id] as? Long

    fun valueOrDefault() = value ?: defaultTtl

    @Composable
    override fun GetContent(
        navController: NavController,
        onError: (Throwable?) -> Unit,
        onValueChange: (Any?) -> Unit
    ) {
        val viewModel = viewModel<SwapDeadlineViewModel>(initializer = {
            SwapDeadlineViewModel(SwapDeadlineService(value, defaultTtl))
        })

        val error = viewModel.errorState?.error

        LaunchedEffect(error) {
            onError.invoke(error)
        }

        TransactionDeadlineInput(
            viewModel.inputFieldPlaceholder,
            viewModel.initialValue,
            viewModel.inputButtons,
            error
        ) {
            viewModel.onChangeText(it)
            onValueChange.invoke(it.toLongOrNull()?.times(60))
        }
    }
}

class SwapDeadlineService(
    override val initialDeadline: Long?,
    override val defaultDeadline: Long,
) : ISwapDeadlineService {

    override var deadlineError: Throwable? = null
    override val deadlineErrorObservable = PublishSubject.create<Optional<Throwable>>()
    override val recommendedDeadlineBounds = Range(600L, 1800L)

    override fun setDeadline(value: Long) {
        deadlineError = if (value == 0L) {
            SwapSettingsModule.SwapSettingsError.ZeroDeadline
        } else {
            null
        }
        deadlineErrorObservable.onNext(Optional.ofNullable(deadlineError))
    }
}
