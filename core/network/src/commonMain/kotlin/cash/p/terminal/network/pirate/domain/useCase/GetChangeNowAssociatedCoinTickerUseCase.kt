package cash.p.terminal.network.pirate.domain.useCase

import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository

class GetChangeNowAssociatedCoinTickerUseCase(
    private val placeRepository: PiratePlaceRepository
) {
    suspend operator fun invoke(coinUid: String, blockchain: String): String? =
        placeRepository.getChangeNowCoinAssociation(coinUid)
            .find { it.blockchain == blockchain }?.ticker
}