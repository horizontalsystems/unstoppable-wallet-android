package cash.p.terminal.network.pirate.data.repository

import cash.p.terminal.network.pirate.api.PirateApi
import cash.p.terminal.network.pirate.data.mapper.MasterNodesMapper
import cash.p.terminal.network.pirate.domain.enity.MasterNodes
import cash.p.terminal.network.pirate.domain.repository.MasterNodesRepository

internal class MasterNodesRepositoryImpl(
    private val masterNodesMapper: MasterNodesMapper,
    private val pirateApi: PirateApi
) : MasterNodesRepository {
    override suspend fun getMasterNodes(): MasterNodes {
        return masterNodesMapper.map(pirateApi.getCoinInfo())
    }
}