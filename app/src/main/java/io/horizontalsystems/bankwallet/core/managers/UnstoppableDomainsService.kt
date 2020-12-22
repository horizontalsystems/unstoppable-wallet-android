package io.horizontalsystems.bankwallet.core.managers

import com.unstoppabledomains.resolution.Resolution
import io.reactivex.Single

class UnstoppableDomainsService {
    private var resolution = Resolution()

    fun resolveDomain(domain: String, ticker: String): Single<String> {
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
