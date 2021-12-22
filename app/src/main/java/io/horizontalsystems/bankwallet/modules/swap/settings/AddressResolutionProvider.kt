package io.horizontalsystems.bankwallet.modules.swap.settings

import com.unstoppabledomains.resolution.Resolution
import io.reactivex.Single

class AddressResolutionProvider {
    private var resolution = Resolution()

    fun isValidAsync(domain: String) = Single.fromCallable {
        resolution.isSupported(domain)
    }

    fun resolveAsync(domain: String, ticker: String): Single<String> {
        return Single.create { emitter ->
            try {
                val address = resolution.getAddress(domain, ticker)
                emitter.onSuccess(address)
            } catch (err: Exception) {
                emitter.onError(err)
            }
        }
    }
}
