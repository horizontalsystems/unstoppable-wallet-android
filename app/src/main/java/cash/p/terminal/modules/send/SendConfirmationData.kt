package cash.p.terminal.modules.send

import cash.p.terminal.entities.Address
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.Coin
import java.math.BigDecimal

data class SendConfirmationData(
    val amount: BigDecimal,
    val fee: BigDecimal,
    val address: Address,
    val coin: Coin,
    val feeCoin: Coin,
    val lockTimeInterval: LockTimeInterval? = null,
    val memo: String? = null,
)
