package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.walletkit.core.storage.EvmSyncSourceStorage
import io.horizontalsystems.walletkit.entities.EvmSyncSource
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Auto-Select for a chain whose kit falls over between its built-in sources by itself (the EVM
 * chains and Tron): the setting and its default, the order the kit tries the sources in, and
 * the startup probe that moves the preferred source.
 */
class EvmSyncSourceAutoSelect(
    private val sources: EvmSyncSourceManager,
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val evmSyncSourceStorage: EvmSyncSourceStorage,
) {
    /**
     * Whether the kit may fall over to the other built-in sources when the selected one fails.
     *
     * Decided once per chain and then stored: on only for a user who never touched this chain's
     * source (still on the default one and no custom RPC added). Anyone who picked another source
     * made a choice that must not be overridden silently.
     */
    fun isEnabled(blockchainType: BlockchainType): Boolean = when {
        // Not stored for an unsupported chain: the default must be derived once it is supported.
        !isSupported(blockchainType) -> false
        else -> blockchainSettingsStorage.evmAutoSelect(blockchainType) ?: storeDefault(blockchainType)
    }

    /** A chain needs at least two built-in sources for its kit to fall over between. */
    fun isSupported(blockchainType: BlockchainType): Boolean =
        sources.defaultSyncSources(blockchainType).size >= 2

    private fun storeDefault(blockchainType: BlockchainType): Boolean {
        val enabled = isUntouched(
            defaults = sources.defaultSyncSources(blockchainType),
            storedUrl = blockchainSettingsStorage.evmSyncSourceUrl(blockchainType),
            hasCustomSources = evmSyncSourceStorage.evmSyncSources(blockchainType).isNotEmpty(),
        )
        blockchainSettingsStorage.saveEvmAutoSelect(enabled, blockchainType)
        return enabled
    }

    fun setEnabled(enabled: Boolean, blockchainType: BlockchainType) {
        if (!isSupported(blockchainType)) return
        blockchainSettingsStorage.saveEvmAutoSelect(enabled, blockchainType)

        val defaults = sources.defaultSyncSources(blockchainType)
        if (enabled && sources.getSyncSource(blockchainType) !in defaults && defaults.isNotEmpty()) {
            // Only built-in sources take part; a custom selection gives way to the default one.
            blockchainSettingsStorage.save(defaults.first().uri.toString(), blockchainType)
        }

        // The kit is rebuilt with or without the fallback sources.
        sources.selectionChanged(blockchainType)
    }

    /**
     * Startup probe: moves the chain to a clearly better built-in source (see [SourceRepick]).
     * The kit has already started on the stored source and is not waited on; a switch rebuilds
     * it once through [EvmSyncSourceManager.syncSourceFlow].
     */
    suspend fun repickOnStartup(
        blockchainType: BlockchainType,
        lagThreshold: Long,
        probe: suspend (List<EvmSyncSource>) -> List<SourceProbe<EvmSyncSource>>,
    ) {
        if (!isEnabled(blockchainType)) return

        val current = sources.getSyncSource(blockchainType)
        val winner = failoverSyncSources(blockchainType)
            .takeIf { it.size >= 2 }
            ?.let { candidates -> withTimeoutOrNull(STARTUP_PROBE_TIMEOUT) { probe(candidates) } }
            ?.let { probes -> SourceRepick.choose(current, probes, lagThreshold) }

        // The user may have changed the source or the setting while the probe ran.
        if (winner != null && isEnabled(blockchainType) && sources.getSyncSource(blockchainType) == current) {
            sources.saveAutoSelected(winner, blockchainType)
        }
    }

    /**
     * The sources the kit tries, in order: the selected one first and, with Auto-Select on, the
     * other built-in HTTP sources after it in their default order.
     */
    fun failoverSyncSources(blockchainType: BlockchainType): List<EvmSyncSource> =
        failoverOrder(
            selected = sources.getSyncSource(blockchainType),
            defaults = sources.defaultSyncSources(blockchainType),
            autoSelect = isEnabled(blockchainType),
        )

    companion object {
        private val STARTUP_PROBE_TIMEOUT = 8.seconds

        /** A user who is still on the first built-in source and never added a custom one. */
        internal fun isUntouched(defaults: List<EvmSyncSource>, storedUrl: String?, hasCustomSources: Boolean): Boolean {
            val default = defaults.firstOrNull() ?: return false
            val onDefaultSource = storedUrl == null || storedUrl == default.uri.toString()
            return onDefaultSource && !hasCustomSources
        }

        internal fun failoverOrder(
            selected: EvmSyncSource,
            defaults: List<EvmSyncSource>,
            autoSelect: Boolean,
        ): List<EvmSyncSource> {
            if (!autoSelect || selected.isWebSocket || selected !in defaults) return listOf(selected)

            // A built-in source with auth takes part too: the kit sends each source's secret
            // only to that source's own URLs.
            return listOf(selected) + defaults.filter { it != selected && it.isHttp }
        }
    }
}
