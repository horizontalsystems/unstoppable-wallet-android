package cash.p.terminal.modules.receive.viewmodels

import cash.p.terminal.core.ServiceState
import cash.p.terminal.core.factories.uriScheme
import cash.p.terminal.core.utils.AddressUriParser
import cash.p.terminal.entities.AddressUri
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import java.math.BigDecimal

class AddressUriService(val token: Token) : ServiceState<AddressUriService.State>() {
    private var address = ""
    private var amount: BigDecimal? = null
    private var uri = ""
    private val parser = AddressUriParser(token.blockchainType, token.type)

    override fun createState() = State(uri = uri)

    fun setAddress(address: String) {
        this.address = address

        refreshUri()

        emitState()
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        refreshUri()

        emitState()
    }

    private fun refreshUri() {
        val tmpAmount = amount

        uri = if (tmpAmount == null || tmpAmount <= BigDecimal.ZERO) {
            address
        } else {
            val addressUri = AddressUri(token.blockchainType.uriScheme ?: "")
            addressUri.address = address

            addressUri.parameters[AddressUri.Field.amountField(token.blockchainType)] =
                tmpAmount.toString()
            addressUri.parameters[AddressUri.Field.BlockchainUid] = token.blockchainType.uid
            if (token.type !is TokenType.Derived && token.type !is TokenType.AddressTyped) {
                addressUri.parameters[AddressUri.Field.TokenUid] = token.type.id
            }

            parser.uri(addressUri)
        }
    }

    data class State(
        val uri: String,
    )
}
