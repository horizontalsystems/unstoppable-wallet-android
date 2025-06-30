package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent

class TransferEventFactory {

    fun transferEvents(transactionRecord: TransactionRecord): List<TransferEvent> {
        return when (transactionRecord) {
            is EvmIncomingTransactionRecord -> {
                listOf(TransferEvent(transactionRecord.from, transactionRecord.value))
            }

            is ExternalContractCallTransactionRecord -> {
                transactionRecord.incomingEvents + transactionRecord.outgoingEvents
            }

            else -> {
                listOf()
            }
        }
    }
}