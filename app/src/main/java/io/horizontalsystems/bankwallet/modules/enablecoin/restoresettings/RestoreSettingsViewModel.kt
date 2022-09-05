package io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable
import kotlinx.parcelize.Parcelize

class RestoreSettingsViewModel(
    private val service: RestoreSettingsService,
    private val clearables: List<Clearable>
) : ViewModel() {

    var openZcashConfigure by mutableStateOf<Token?>(null)
        private set

    private var disposables = CompositeDisposable()

    private var currentRequest: RestoreSettingsService.Request? = null

    init {
        service.requestObservable
                .subscribeIO {
                    handleRequest(it)
                }
                .let {
                    disposables.add(it)
                }
    }

    private fun handleRequest(request: RestoreSettingsService.Request) {
        currentRequest = request

        when (request.requestType) {
            RestoreSettingsService.RequestType.BirthdayHeight -> {
                openZcashConfigure = request.token
            }
        }
    }

    fun onEnter(zcashConfig: ZCashConfig) {
        val request = currentRequest ?: return

        when (request.requestType) {
            RestoreSettingsService.RequestType.BirthdayHeight -> {
                service.enter(zcashConfig, request.token)
            }
        }
    }

    fun onCancelEnterBirthdayHeight() {
        val request = currentRequest ?: return

        service.cancel(request.token)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    fun zcashConfigureOpened() {
        openZcashConfigure = null
    }
}

@Parcelize
data class ZCashConfig(val birthdayHeight: String?, val restoreAsNew: Boolean) : Parcelable
