package cash.p.terminal.network.pirate.data.mapper

import cash.p.terminal.network.pirate.data.entity.MasterNodesDto
import cash.p.terminal.network.pirate.domain.enity.MasterNodes

internal class MasterNodesMapper {
    fun map(dto: MasterNodesDto): MasterNodes {
        return MasterNodes(
            ips = dto.ips
        )
    }
}