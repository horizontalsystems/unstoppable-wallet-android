package cash.p.terminal.tangem.domain.task

import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve

class CardInitializationValidator(private val expectedCurves: List<EllipticCurve>) {

    fun validateWallets(wallets: List<CardWallet>): Boolean {
        val curves = wallets.map { it.curve }.toSet()
        return curves.size == expectedCurves.size &&
                curves.containsAll(expectedCurves)
    }
}
