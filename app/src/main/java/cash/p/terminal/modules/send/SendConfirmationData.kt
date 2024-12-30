package cash.p.terminal.modules.send

import cash.p.terminal.entities.Address
import cash.p.terminal.modules.contacts.model.Contact
import io.horizontalsystems.hodler.LockTimeInterval
import cash.p.terminal.wallet.entities.Coin
import java.math.BigDecimal

data class SendConfirmationData(
    val amount: BigDecimal,
    val fee: BigDecimal,
    val address: Address,
    val contact: Contact?,
    val coin: Coin,
    val feeCoin: Coin,
    val lockTimeInterval: LockTimeInterval? = null,
    val memo: String?,
    val rbfEnabled: Boolean? = null
)
