package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class RestoreSettingsViewModel(
        private val service: RestoreSettingsService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val openBirthdayAlertSignal = SingleLiveEvent<String>()
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
            RestoreSettingsService.RequestType.birthdayHeight -> {
                openBirthdayAlertSignal.postValue(request.coin.title)
            }
        }
    }

    fun onEnter(birthdayHeight: String?) {
        val request = currentRequest ?: return

        when (request.requestType) {
            RestoreSettingsService.RequestType.birthdayHeight -> {
                service.enter(birthdayHeight, request.coin)
            }
        }
    }

    fun onCancelEnterBirthdayHeight() {
        val request = currentRequest ?: return

        service.cancel(request.coin)
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }
}
