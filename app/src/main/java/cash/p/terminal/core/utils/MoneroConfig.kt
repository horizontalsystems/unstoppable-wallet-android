package cash.p.terminal.core.utils

import com.m2049r.xmrwallet.data.DefaultNodes
import com.m2049r.xmrwallet.data.NodeInfo
import com.m2049r.xmrwallet.util.NodePinger.execute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.Collections

object MoneroConfig {
    const val WORD_COUNT = 25

    suspend fun autoSelectNode(): NodeInfo? = withContext(Dispatchers.IO) {
        try {
            val deferred = async {
                DefaultNodes.entries.map { NodeInfo.fromString(it.uri) }.toSet()
            }

            val nodes = withTimeoutOrNull(10_000) {
                deferred.await()
            }
            if (nodes.isNullOrEmpty()) return@withContext null
            execute(nodes, null)
            val nodeList: MutableList<NodeInfo?> = ArrayList(nodes)
            Collections.sort<NodeInfo?>(nodeList, NodeInfo.BestNodeComparator)
            return@withContext nodeList[0]
        } catch (ex: Exception) {
            Timber.d(ex)
            return@withContext null
        }
    }
}