package io.horizontalsystems.bankwallet.core.managers

import android.net.Uri
import androidx.core.net.toUri
import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.core.storage.MoneroNodeStorage
import io.horizontalsystems.bankwallet.entities.MoneroNodeRecord
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MoneroNodeManager(
    private val blockchainSettingsStorage: BlockchainSettingsStorage,
    private val moneroNodeStorage: MoneroNodeStorage,
    private val marketKitWrapper: MarketKitWrapper
) {
    private val blockchainType = BlockchainType.Monero

    private val _currentNodeUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val currentNodeUpdatedFlow = _currentNodeUpdatedFlow.asSharedFlow()

    private val _nodesUpdatedFlow = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nodesUpdatedFlow = _nodesUpdatedFlow.asSharedFlow()

    val defaultNodes: List<MoneroNode> =
        listOf(
            MoneroNode("xmr-de.boldsuck.org:18081", "boldsuck.org", "xmr-de.boldsuck.org:18081/mainnet/boldsuck.org"),
            MoneroNode("node.sethforprivacy.com:18089", "sethforprivacy.com", "node.sethforprivacy.com:18089/mainnet/sethforprivacy.com"),
            MoneroNode("node.xmr.rocks:18089", "xmr.rocks", "node.xmr.rocks:18089/mainnet/xmr.rocks"),
            MoneroNode("node.monerodevs.org:18089", "monerodevs.org", "node.monerodevs.org:18089/mainnet/monerodevs.org"),
            MoneroNode("nodex.monerujo.io:18081", "monerujo.io", "nodex.monerujo.io:18081/mainnet/monerujo.io"),
            MoneroNode("xmr-node.cakewallet.com:18081", "cakewallet.com", "xmr-node.cakewallet.com:18081/mainnet/cakewallet.com"),
            MoneroNode("monero.stackwallet.com:18081", "stackwallet.com", "monero.stackwallet.com:18081/mainnet/stackwallet.com"),
        )

    val customNodes: List<MoneroNode>
        get() {
            val records = moneroNodeStorage.getAll()
            return try {
                records.mapNotNull { record ->
                    val uri = record.url.toUri()
                    MoneroNode(
                        host = record.url,
                        name = uri.host ?: "",
                        serialized = serializeNode(uri, record.username, record.password)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    val allNodes: List<MoneroNode>
        get() = defaultNodes + customNodes

    val currentNode: MoneroNode
        get() {
            val moneroNodeHost = blockchainSettingsStorage.moneroNodeHost()
            val rpcSource = allNodes.firstOrNull { it.host == moneroNodeHost }

            return rpcSource ?: defaultNodes.first()
        }

    val blockchain: Blockchain?
        get() = marketKitWrapper.blockchain(blockchainType.uid)

    fun save(node: MoneroNode) {
        blockchainSettingsStorage.saveMoneroNode(node.host)
        _currentNodeUpdatedFlow.tryEmit(node.host)
    }

    private fun serializeNode(uri: Uri, username: String?, password: String?): String {
        return "$username:$password@${uri.host}:${uri.port}/mainnet/${uri.host ?: ""}"
    }

    fun addMoneroNode(url: String, username: String?, password: String?) {
        val record = MoneroNodeRecord(
            url = url,
            username = username,
            password = password
        )

        moneroNodeStorage.save(record)

        customNodes.firstOrNull { it.host == url }?.let {
            save(it)
        }

        _nodesUpdatedFlow.tryEmit(url)
    }

    fun delete(node: MoneroNode) {
        val isCurrent = node == currentNode

        moneroNodeStorage.delete(node.host)

        if (isCurrent) {
            _currentNodeUpdatedFlow.tryEmit(node.host)
        }

        _nodesUpdatedFlow.tryEmit(node.host)
    }

    data class MoneroNode(
        val host: String,
        val name: String,
        val serialized: String
    ) {
        override fun equals(other: Any?): Boolean {
            return other is MoneroNode && other.host == this.host
        }

        override fun hashCode(): Int {
            return this.host.hashCode()
        }
    }
}