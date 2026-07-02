package io.horizontalsystems.core.core.factories

import io.horizontalsystems.core.core.adapters.StellarTransactionRecord
import io.horizontalsystems.core.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.core.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import io.horizontalsystems.core.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import io.horizontalsystems.core.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.core.entities.transactionrecords.tron.TronExternalContractCallTransactionRecord
import io.horizontalsystems.core.entities.transactionrecords.tron.TronIncomingTransactionRecord

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