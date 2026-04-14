package com.quantum.wallet.bankwallet.core.factories

import com.quantum.wallet.bankwallet.core.adapters.StellarTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.TransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.TransferEvent
import com.quantum.wallet.bankwallet.entities.transactionrecords.tron.TronExternalContractCallTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.tron.TronIncomingTransactionRecord

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