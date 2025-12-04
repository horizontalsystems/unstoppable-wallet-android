package io.horizontalsystems.bankwallet.modules.receive.viewmodels

import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.core.chainId
import io.horizontalsystems.bankwallet.core.factories.uriScheme
import io.horizontalsystems.bankwallet.core.isEvm
import io.horizontalsystems.bankwallet.core.utils.AddressUriParser
import io.horizontalsystems.bankwallet.entities.AddressUri
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal
import java.math.RoundingMode

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
        if (token.blockchainType.isEvm) {
            // set amount in wei and remove any fractional part
            this.amount = amount?.movePointRight(18)?.setScale(0, RoundingMode.DOWN)
        } else {
            this.amount = amount
        }

        refreshUri()

        emitState()
    }

    private fun refreshUri() {
        val tmpAmount = amount

        uri = if (tmpAmount == null || tmpAmount <= BigDecimal.ZERO) {
            address
        } else {
            val addressUri = AddressUri(token.blockchainType.uriScheme ?: "")
            //if blockchainType of token is EVM we need to attach chainId with prefix @ after address
            //else we should use old method to add Field.BlockchainUid parameter
            var address = this.address
            if (token.blockchainType !is BlockchainType.Ethereum) {
                if (token.blockchainType.isEvm) {
                    // attach chain id after address with '@' prefix
                    address += token.blockchainType.chainId?.let { "@${it}" }
                } else {
                    addressUri.parameters[AddressUri.Field.BlockchainUid] = token.blockchainType.uid
                }
            }

            addressUri.address = address

            addressUri.parameters[AddressUri.Field.amountField(token.blockchainType)] = tmpAmount.toString()
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
