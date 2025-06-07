package cash.p.terminal.tangem.domain.task.reponse

import cash.p.terminal.tangem.domain.model.KeyWalletPublicKey
import com.tangem.common.card.Card
import com.tangem.operations.CommandResponse
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.ExtendedPublicKeysMap

data class CreateProductWalletTaskResponse(
    val card: Card,
    val derivedKeys: Map<KeyWalletPublicKey, ExtendedPublicKeysMap> = mapOf(),
    val primaryCard: PrimaryCard? = null,
) : CommandResponse
