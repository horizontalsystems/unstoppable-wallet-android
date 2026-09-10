package io.horizontalsystems.walletkit.modules.send.bitcoin.v2

import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.walletkit.core.chain.SendChainSettings
import io.horizontalsystems.marketkit.models.BlockchainType

/**
 * Per-send choices from the Bitcoin settings page: the outputs to spend (null for automatic
 * selection) and a timelock. Sorting and replace-by-fee are persisted preferences and are
 * read from storage when the transfer is built.
 */
data class BtcSendSettings(
    val unspentOutputs: List<UnspentOutputInfo>? = null,
    val lockTimeInterval: LockTimeInterval? = null,
) : SendChainSettings {

    /**
     * Hodler plugin data for the transfer, or null. A chosen interval is dropped silently
     * when the recipient cannot carry it: the kit only locks legacy (P2PKH) outputs, and the
     * settings page shows the row disabled for other recipients.
     */
    fun pluginData(blockchainType: BlockchainType, address: String): Map<Byte, IPluginData>? {
        val interval = lockTimeInterval ?: return null
        if (!timeLockAvailable(blockchainType, address)) return null
        return mapOf(HodlerPlugin.id to HodlerData(interval))
    }

    companion object {
        /** The hodler plugin exists only in the Bitcoin kit; the row is hidden elsewhere. */
        fun timeLockSupported(blockchainType: BlockchainType) = blockchainType == BlockchainType.Bitcoin

        /** Locking needs a legacy recipient address, the ones starting with "1". */
        fun timeLockAvailable(blockchainType: BlockchainType, address: String?) =
            timeLockSupported(blockchainType) && address?.startsWith("1") == true
    }
}
