package io.horizontalsystems.walletkit.modules.transactionInfo

import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord

// One-shot in-memory handoff of the tapped record to TransactionInfoPage.
// TransactionRecord is not serializable and cannot be re-fetched by id, so it
// cannot travel inside the nav page itself. After process death this is null
// and the restored page pops itself instead of resolving a shared ViewModel
// whose store no longer exists.
object TransactionInfoPayload {
    var record: TransactionRecord? = null
}
