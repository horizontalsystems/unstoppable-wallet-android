package io.horizontalsystems.walletkit.chain.zcash

import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashAdapter.ZcashSubmitException
import io.horizontalsystems.walletkit.core.chain.NodeError

/**
 * Whether a failed Zcash send was the server's failure. A proposal is built from the wallet's
 * own data, so only the submission reaches the server: when that call fails the transaction
 * is already built and kept by the wallet, and it may have been received, or be sent again by
 * the wallet later. A transaction the server answered and refused is not a server failure.
 */
internal fun zcashNodeError(error: Throwable): NodeError? =
    generateSequence(error) { it.cause }
        .filterIsInstance<ZcashSubmitException>()
        .firstOrNull()
        ?.takeIf { it.grpcError }
        ?.let { NodeError(preBroadcast = false) }
