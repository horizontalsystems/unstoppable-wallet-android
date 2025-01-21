package io.horizontalsystems.bankwallet.core.address

import io.horizontalsystems.bankwallet.entities.Address

class AddressSecurityCheckerChain {

    interface IAddressSecurityCheckerItem {
        suspend fun handle(address: Address): SecurityIssue?
    }

    private val handlers: MutableList<IAddressSecurityCheckerItem> = mutableListOf()

    fun append(handlers: List<IAddressSecurityCheckerItem>): AddressSecurityCheckerChain {
        this.handlers.addAll(handlers)
        return this
    }

    fun append(handler: IAddressSecurityCheckerItem): AddressSecurityCheckerChain {
        handlers.add(handler)
        return this
    }

    suspend fun handle(address: Address): List<SecurityIssue> {
        return handlers.mapNotNull { handler -> handler.handle(address) }
    }

    sealed class SecurityIssue {
        data class Spam(val transactionHash: String) : SecurityIssue()
        data class Sanctioned(val description: String) : SecurityIssue()

        override fun toString(): String =
            when (this) {
                is Spam -> "Possibly phishing address. Transaction hash: $transactionHash"
                is Sanctioned -> description
            }
    }

}