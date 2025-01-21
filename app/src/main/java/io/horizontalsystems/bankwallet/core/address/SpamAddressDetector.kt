package io.horizontalsystems.bankwallet.core.address

import io.horizontalsystems.bankwallet.core.address.AddressSecurityCheckerChain.SecurityIssue
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.entities.Address

class SpamAddressDetector(
    private val spamManager: SpamManager
) : AddressSecurityCheckerChain.IAddressSecurityCheckerItem {

    override suspend fun handle(address: Address): SecurityIssue? {
        val spamAddress = spamManager.find(address.hex.uppercase())
        return spamAddress?.let { SecurityIssue.Spam(it.transactionHash.toHexString()) }
    }

}
