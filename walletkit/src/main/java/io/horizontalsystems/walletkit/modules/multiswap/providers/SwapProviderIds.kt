package io.horizontalsystems.walletkit.modules.multiswap.providers

import io.horizontalsystems.walletkit.entities.SwapRecord
import io.horizontalsystems.walletkit.modules.multiswap.history.AllBridgeTracker

/**
 * The provider id a swap record carries, read as the server names the provider. Records made
 * since the app quoted one `/v3/rate` for every venue store the server id itself (`NEAR`);
 * older ones carry the `u_` prefix the app used to add (`u_NEAR`), or the id of a provider the
 * app quoted natively before it moved behind the server (`thorchain`, `oneinch`). History and
 * tracking resolve every record through here, so neither form needs a migration.
 */
object SwapProviderIds {
    private const val LEGACY_PREFIX = "u_"

    private val legacyNativeIds = mapOf(
        THORCHAIN_PROVIDER_ID to "THORCHAIN",
        MAYA_PROVIDER_ID to "MAYACHAIN",
        ONEINCH_PROVIDER_ID to "ONEINCH",
        UNISWAP_V3_PROVIDER_ID to "UNISWAP_V3",
        PANCAKE_V3_PROVIDER_ID to "PANCAKESWAP",
        AllBridgeTracker.PROVIDER_ID to "ALLBRIDGE",
    )

    /** The server's id for a record's provider id, whichever form the record carries. */
    fun serverId(recordProviderId: String): String =
        legacyNativeIds[recordProviderId] ?: recordProviderId.removePrefix(LEGACY_PREFIX).uppercase()

    /** Whether the record was made by a provider the app quoted itself, with no server-side order to track by uuid. */
    fun isLegacyNative(recordProviderId: String): Boolean = recordProviderId in legacyNativeIds

    /**
     * Whether the record's swap was one transaction (a same-chain contract call) rather than a
     * deposit followed by a delivery, which decides the stage layout in history. For a server
     * venue the execution type published by `/v3/providers` decides: a `signed_transaction`
     * route on one chain is a single transaction, anything cross-chain or settled through a
     * deposit is not.
     */
    fun isSingleTransactionSwap(record: SwapRecord, executionType: (serverId: String) -> String?): Boolean {
        val sameChain = record.tokenInBlockchainTypeUid == record.tokenOutBlockchainTypeUid
        return when (record.providerId) {
            ONEINCH_PROVIDER_ID, UNISWAP_V3_PROVIDER_ID, PANCAKE_V3_PROVIDER_ID -> true
            THORCHAIN_PROVIDER_ID, MAYA_PROVIDER_ID -> false
            AllBridgeTracker.PROVIDER_ID -> sameChain
            else -> sameChain && executionType(serverId(record.providerId)) == SIGNED_TRANSACTION_EXECUTION_TYPE
        }
    }

    const val SIGNED_TRANSACTION_EXECUTION_TYPE = "signed_transaction"
    const val TRANSFER_EXECUTION_TYPE = "transfer"
}
