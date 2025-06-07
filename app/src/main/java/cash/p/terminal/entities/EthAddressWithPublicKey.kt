package cash.p.terminal.entities
import io.horizontalsystems.ethereumkit.models.Address

class EthAddressWithPublicKey(
    val address: Address,
    val publicKey: ByteArray
)