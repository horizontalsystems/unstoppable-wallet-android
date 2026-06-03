package cash.p.terminal.core.usecase

import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum
import cash.p.terminal.network.changenow.domain.entity.toStatus
import cash.p.terminal.network.data.EncodedSecrets.getKoin
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.network.swaprepository.SwapProviderTransactionStatusRepository
import cash.p.terminal.network.swaprepository.SwapProviderTransactionStatusResult
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.koin.core.qualifier.named
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class UpdateSwapProviderTransactionsStatusUseCase(
    private val swapProviderTransactionsStorage: SwapProviderTransactionsStorage,
    private val accountManager: IAccountManager
) {
    suspend operator fun invoke(
        token: Token,
        address: String
    ): Boolean = withContext(Dispatchers.IO) {
        pollAndUpdate(
            swapProviderTransactionsStorage.getAll(
                token = token,
                address = address,
                statusesExcluded = SwapProviderTransaction.FINISHED_STATUSES,
                limit = STATUS_CHECK_LIMIT
            )
        )
    }

    suspend operator fun invoke(): Boolean = withContext(Dispatchers.IO) {
        val accountId = accountManager.activeAccount?.id ?: return@withContext false
        pollAndUpdate(
            swapProviderTransactionsStorage.getAllUnfinishedByAccount(
                accountId = accountId,
                statusesExcluded = SwapProviderTransaction.FINISHED_STATUSES,
                limit = STATUS_CHECK_LIMIT
            )
        )
    }

    private suspend fun pollAndUpdate(
        transactions: List<SwapProviderTransaction>
    ): Boolean = coroutineScope {
        val changed = AtomicBoolean(false)
        transactions.map { transaction ->
            async {
                getTransactionStatus(transaction)?.let { result ->
                    if (updateIfChanged(transaction, result)) {
                        changed.set(true)
                    }
                }
            }
        }.awaitAll()
        changed.get()
    }

    suspend fun updateTransactionStatus(
        transactionId: String
    ): TransactionStatusEnum? = withContext(Dispatchers.IO) {
        swapProviderTransactionsStorage.getTransaction(transactionId)?.let { transaction ->
            if (!transaction.isFinished()) {
                getTransactionStatus(transaction)?.let { result ->
                    updateIfChanged(transaction, result)
                }
            }
            swapProviderTransactionsStorage.getTransaction(transactionId)?.status?.toStatus()
        }
    }

    private fun updateIfChanged(
        transaction: SwapProviderTransaction,
        result: SwapProviderTransactionStatusResult
    ): Boolean {
        val statusChanged = result.status.name.lowercase() != transaction.status
        // Don't consider amountOutReal changed if already matched (has actual blockchain amount)
        val amountOutRealChanged = transaction.incomingRecordUid == null &&
                result.amountOutReal != null && result.amountOutReal != transaction.amountOutReal
        val finishedAtChanged = result.finishedAt != null && result.finishedAt != transaction.finishedAt

        return if (statusChanged || amountOutRealChanged || finishedAtChanged) {
            swapProviderTransactionsStorage.updateStatusFields(
                transactionId = transaction.transactionId,
                status = result.status.name.lowercase(),
                // Keep actual blockchain amount if already matched
                amountOutReal = if (transaction.incomingRecordUid != null) {
                    transaction.amountOutReal
                } else {
                    result.amountOutReal ?: transaction.amountOutReal
                },
                finishedAt = result.finishedAt ?: transaction.finishedAt
            )
            true
        } else {
            false
        }
    }

    private suspend fun getTransactionStatus(
        transaction: SwapProviderTransaction
    ): SwapProviderTransactionStatusResult? = try {
        getSwapProviderTransactionStatusRepository(transaction.provider)
            ?.getTransactionStatus(
                transactionId = transaction.transactionId,
                destinationAddress = transaction.addressOut
            ) ?: run {
            Timber.d("Transaction status repository not found for provider: ${transaction.provider}")
            null
        }
    } catch (e: Throwable) {
        Timber.d("Failed to get transaction status for id: ${transaction.transactionId}")
        e.printStackTrace()
        null
    }

    private fun getSwapProviderTransactionStatusRepository(provider: SwapProvider): SwapProviderTransactionStatusRepository? {
        return try {
            getKoin().get(named(provider))
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val STATUS_CHECK_LIMIT = 10
    }
}
