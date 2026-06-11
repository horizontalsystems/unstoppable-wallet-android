package cash.p.terminal.network.exolix.data.mapper

import cash.p.terminal.network.exolix.data.entity.ExolixNetworkDto
import cash.p.terminal.network.exolix.data.entity.ExolixRateDto
import cash.p.terminal.network.exolix.data.entity.ExolixTransactionDto
import cash.p.terminal.network.exolix.domain.entity.ExolixNetwork
import cash.p.terminal.network.exolix.domain.entity.ExolixRate
import cash.p.terminal.network.exolix.domain.entity.ExolixTransaction

internal class ExolixMapper {
    fun mapNetworksDto(list: List<ExolixNetworkDto>) =
        list.map { network ->
            ExolixNetwork(
                network = network.network,
                name = network.name,
                shortName = network.shortName,
                memoNeeded = network.memoNeeded,
                memoName = network.memoName,
                contract = network.contract,
            )
        }

    fun mapRateDto(dto: ExolixRateDto) = ExolixRate(
        fromAmount = dto.fromAmount,
        toAmount = dto.toAmount,
        rate = dto.rate,
        minAmount = dto.minAmount,
        withdrawMin = dto.withdrawMin,
        maxAmount = dto.maxAmount,
    )

    fun mapTransactionDto(dto: ExolixTransactionDto) = ExolixTransaction(
        id = dto.id,
        amount = dto.amount,
        amountTo = dto.amountTo,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        depositAddress = dto.depositAddress,
        depositExtraId = dto.depositExtraId,
        withdrawalAddress = dto.withdrawalAddress,
        withdrawalExtraId = dto.withdrawalExtraId,
        status = dto.status,
    )
}
