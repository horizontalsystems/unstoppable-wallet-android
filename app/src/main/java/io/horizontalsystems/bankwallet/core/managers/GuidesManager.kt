package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.entities.Guide
import java.util.*

object GuidesManager {

    val guides: List<Guide> = listOf(
            Guide(
                    title = "Tether in Simple Terms",
                    date = Date(),
                    imageUrl = "https://raw.githubusercontent.com/horizontalsystems/blockchain-crypto-guides/master/token_guides/images/TetherMain.png",
                    fileName = "tether"
            ),
            Guide(
                    title = "MakerDAO & DAI in Simple Terms",
                    date = Date(),
                    imageUrl = "http://media.gettyimages.com/photos/car-steel-wheels-of-a-new-bmw-coupe-picture-id516914879",
                    fileName = "maker"
            ),
            Guide(
                    title = "Bitcoin In Simple Terms",
                    date = Date(),
                    imageUrl = "https://media.gettyimages.com/photos/530d-car-head-lights-picture-id157735154",
                    fileName = "bitcoin"
            ),
            Guide(
                    title = "Ethereum in Simple Terms",
                    date = Date(),
                    imageUrl = "http://media.gettyimages.com/photos/modern-key-to-the-bmw-in-a-hand-picture-id890886864",
                    fileName = "ethereum"
            )
    )
}
