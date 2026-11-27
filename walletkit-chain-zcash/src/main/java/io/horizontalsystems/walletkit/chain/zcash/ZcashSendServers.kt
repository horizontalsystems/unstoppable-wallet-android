package io.horizontalsystems.walletkit.chain.zcash

import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.EndpointPingResult
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager.ZcashEndpoint
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

// A ping that never returns must not hold a send
private val PING_TIMEOUT = 8.seconds

/**
 * The other servers a send may go on through when the wallet's own gives no answer: with
 * Auto-Select on, the ones it may pick that answer a ping, fastest first. A server the user
 * picked stays the only one their transfers are sent to. Read at the time of the send: the
 * switch and the servers can change.
 */
internal suspend fun ZcashLightWalletEndpointManager.otherEndpointsThatAnswer(): List<LightWalletEndpoint> {
    if (!autoSelectEnabled) return emptyList()

    val others = allEndpoints - currentEndpoint
    val pings = withTimeoutOrNull(PING_TIMEOUT) { pingEndpoints(others.map { it.url }) }.orEmpty()
    return endpointsThatAnswer(others, pings).map { it.toLightWalletEndpoint() }
}

/** The servers whose ping came back good, fastest first. */
internal fun endpointsThatAnswer(endpoints: List<ZcashEndpoint>, pings: List<EndpointPingResult>): List<ZcashEndpoint> {
    val byUrl = endpoints.associateBy { it.url }
    return pings
        .filter { it.isValid && it.responseTime < Double.MAX_VALUE }
        .sortedBy { it.responseTime }
        .mapNotNull { byUrl[it.url] }
}
