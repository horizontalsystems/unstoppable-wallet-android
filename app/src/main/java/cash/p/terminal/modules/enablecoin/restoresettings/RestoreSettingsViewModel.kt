package cash.p.terminal.modules.enablecoin.restoresettings

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.managers.AccountCleaner
import cash.p.terminal.wallet.Clearable
import cash.p.terminal.wallet.Token
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.parcelize.Parcelize
import org.koin.java.KoinJavaComponent.inject

class RestoreSettingsViewModel(
    private val service: RestoreSettingsService,
    private val clearables: List<Clearable>,
) : ViewModel(), IRestoreSettingsUi {

    override var openTokenConfigure by mutableStateOf<Token?>(null)
        private set

    private var currentRequest: RestoreSettingsService.Request? = null
    private var currentRequestConfig: TokenConfig? = null
    private val queuedRequests = ArrayDeque<RestoreSettingsService.Request>()

    private val accountCleaner: AccountCleaner by inject(AccountCleaner::class.java)

    init {
        viewModelScope.launch {
            service.requestObservable.asFlow().collect {
                handleRequest(it)
            }
        }
    }

    private fun handleRequest(request: RestoreSettingsService.Request) {
        if (currentRequest != null) {
            queuedRequests.addLast(request)
            return
        }

        currentRequest = request
        currentRequestConfig = request.initialConfig

        when (request.requestType) {
            RestoreSettingsService.RequestType.BirthdayHeight -> {
                openTokenConfigure = request.token
            }
        }
    }

    override fun onEnter(tokenConfig: TokenConfig) {
        viewModelScope.launch {
            enter(tokenConfig)
        }
    }

    private suspend fun enter(tokenConfig: TokenConfig) {
        val request = currentRequest ?: return

        when (request.requestType) {
            RestoreSettingsService.RequestType.BirthdayHeight -> {
                val changed =
                    request.initialConfig != null &&
                        request.initialConfig.birthdayHeight != tokenConfig.birthdayHeight
                if (changed) {
                    // A new restore height invalidates previously scanned local wallet data.
                    request.accountId?.let { accountId ->
                        accountCleaner.clearWalletForAccount(accountId, request.token)
                    }
                }
                service.enter(tokenConfig, request.token)
            }
        }
        finishRequest(request)
    }

    override fun onCancelEnterBirthdayHeight() {
        val request = currentRequest ?: return

        service.cancel(request.token)
        finishRequest(request)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
    }

    override fun tokenConfigureOpened() {
        openTokenConfigure = null
    }

    override fun consumeInitialConfig(): TokenConfig? {
        return currentRequestConfig.also {
            currentRequestConfig = null
        }
    }

    private fun finishRequest(request: RestoreSettingsService.Request) {
        if (currentRequest != request) return

        currentRequest = null
        currentRequestConfig = null
        if (queuedRequests.isNotEmpty()) {
            handleRequest(queuedRequests.removeFirst())
        }
    }
}

@Parcelize
data class TokenConfig(val birthdayHeight: String?, val restoreAsNew: Boolean) : Parcelable

interface IRestoreSettingsUi {
    val openTokenConfigure: Token?

    fun tokenConfigureOpened()
    fun consumeInitialConfig(): TokenConfig?
    fun onEnter(tokenConfig: TokenConfig)
    fun onCancelEnterBirthdayHeight()
}
