package cash.p.terminal.network.pirate.domain.repository

import cash.p.terminal.network.pirate.domain.enity.MasterNodes

interface MasterNodesRepository {
    suspend fun getMasterNodes(): MasterNodes
}