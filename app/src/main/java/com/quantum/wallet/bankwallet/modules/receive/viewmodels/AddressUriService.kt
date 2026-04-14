package com.quantum.wallet.bankwallet.modules.receive.viewmodels

import com.quantum.wallet.bankwallet.core.ServiceState
import com.quantum.wallet.bankwallet.core.chainId
import com.quantum.wallet.bankwallet.core.factories.uriScheme
import com.quantum.wallet.bankwallet.core.isEvm
import com.quantum.wallet.bankwallet.core.utils.AddressUriParser
import com.quantum.wallet.bankwallet.entities.AddressUri
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
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
            //if blockchainType of token is EVM we need to attach chainId with prefix @ after address
            //else we should use old method to add Field.BlockchainUid parameter
            var address = this.address
            if (token.blockchainType.isEvm) {
                // attach chain id after address with '@' prefix
                address += token.blockchainType.chainId?.let { "@${it}" }
            } else {
                addressUri.parameters[AddressUri.Field.BlockchainUid] = token.blockchainType.uid
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
