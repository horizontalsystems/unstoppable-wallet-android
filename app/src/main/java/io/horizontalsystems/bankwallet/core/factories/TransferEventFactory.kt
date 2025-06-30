package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.adapters.StellarTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronIncomingTransactionRecord

class TransferEventFactory {

    fun transferEvents(transactionRecord: TransactionRecord): List<TransferEvent> {
        return when (transactionRecord) {
            is EvmIncomingTransactionRecord -> {
                listOf(TransferEvent(transactionRecord.from, transactionRecord.value))
            }

            is ExternalContractCallTransactionRecord -> {
                transactionRecord.incomingEvents + transactionRecord.outgoingEvents
            }

            is TronExternalContractCallTransactionRecord -> {
                transactionRecord.incomingEvents + transactionRecord.outgoingEvents
            }

            is TronIncomingTransactionRecord -> {
                listOf(TransferEvent(transactionRecord.from, transactionRecord.value))
            }

            is StellarTransactionRecord -> {
                StellarTransactionRecord.eventsForPhishingCheck(transactionRecord.type)
            }

            else -> {
                listOf()
            }
        }
    }
}