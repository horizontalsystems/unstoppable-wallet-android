package io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.parcelize.Parcelize

class RestoreSettingsViewModel(
    private val service: RestoreSettingsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    var openBirthdayHeightConfig by mutableStateOf<Token?>(null)
        private set

    private var currentRequest: RestoreSettingsService.Request? = null

    init {
        viewModelScope.launch {
            service.requestObservable.asFlow().collect {
                handleRequest(it)
            }
        }
    }

    private fun handleRequest(request: RestoreSettingsService.Request) {
        currentRequest = request

        when (request.requestType) {
            RestoreSettingsService.RequestType.BirthdayHeight -> {
                openBirthdayHeightConfig = request.token
            }
        }
    }

    fun onEnter(config: BirthdayHeightConfig) {
        val request = currentRequest ?: return

        when (request.requestType) {
            RestoreSettingsService.RequestType.BirthdayHeight -> {
                service.enter(config, request.token)
            }
        }
    }

    fun onCancelEnterBirthdayHeight() {
        val request = currentRequest ?: return

        service.cancel(request.token)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

    fun birthdayHeightConfigOpened() {
        openBirthdayHeightConfig = null
    }
}

@Parcelize
data class BirthdayHeightConfig(val birthdayHeight: String?, val restoreAsNew: Boolean) : Parcelable
