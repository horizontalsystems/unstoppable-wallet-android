package cash.p.terminal.tangem.domain.task

import com.tangem.common.card.Card
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.operations.preflightread.PreflightReadFilter

/**
 * [PreflightReadFilter] for checking if card has expected user wallet id
 */
class UserWalletIdPreflightReadFilter(
    private val expectedPublicKey: ByteArray
) : PreflightReadFilter {

    override fun onCardRead(card: Card, environment: SessionEnvironment) = Unit

    @OptIn(ExperimentalStdlibApi::class)
    override fun onFullCardRead(card: Card, environment: SessionEnvironment) {
        if (card.wallets.any { it.publicKey == expectedPublicKey }) throw TangemSdkError.WalletNotFound()
    }
}