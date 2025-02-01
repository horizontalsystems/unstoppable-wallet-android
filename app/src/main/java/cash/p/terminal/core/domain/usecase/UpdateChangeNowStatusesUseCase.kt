package cash.p.terminal.core.domain.usecase

import cash.p.terminal.core.storage.ChangeNowTransactionsStorage
import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum
import cash.p.terminal.network.changenow.domain.repository.ChangeNowRepository
import cash.p.terminal.wallet.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class UpdateChangeNowStatusesUseCase(
    private val changeNowRepository: ChangeNowRepository,
    private val changeNowTransactionsStorage: ChangeNowTransactionsStorage
) {
    suspend operator fun invoke(
        token: Token,
        address: String
    ): Boolean = withContext(Dispatchers.IO) {
        val changed = AtomicBoolean(false)
        changeNowTransactionsStorage.getAll(
            token = token,
            address = address,
            statuses = listOf(
                TransactionStatusEnum.NEW.name.lowercase(),
                TransactionStatusEnum.WAITING.name.lowercase(),
                TransactionStatusEnum.CONFIRMING.name.lowercase(),
                TransactionStatusEnum.EXCHANGING.name.lowercase(),
                TransactionStatusEnum.SENDING.name.lowercase(),
                TransactionStatusEnum.VERIFYING.name.lowercase()
            ),
            limit = 10
        ).map { transaction ->
            async {
                try {
                    val status = changeNowRepository.getTransactionStatus(transaction.transactionId)
                    if (status.status.name.lowercase() != transaction.status) {
                        changed.set(true)
                        changeNowTransactionsStorage.save(
                            transaction.copy(
                                status = status.status.name.lowercase()
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.awaitAll()
        changed.get()
    }
}
