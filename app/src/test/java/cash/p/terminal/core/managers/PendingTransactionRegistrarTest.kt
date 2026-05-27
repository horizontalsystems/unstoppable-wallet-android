package cash.p.terminal.core.managers

import cash.p.terminal.entities.PendingTransactionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PendingTransactionRegistrarTest {

    @Test
    fun updateTxId_parentCancelled_updatesPendingAndMarksCreated() = runTest {
        val entity = pendingTransactionEntity()
        val repository = mockk<PendingTransactionRepository> {
            coEvery { updateTxId("draft-id", "tx-hash") } returns entity
        }
        val locallyCreatedTransactionRepository = mockk<LocallyCreatedTransactionRepository>(relaxed = true)
        val registrar = PendingTransactionRegistrarImpl(
            repository = repository,
            pendingBalanceCalculator = mockk(relaxed = true),
            locallyCreatedTransactionRepository = locallyCreatedTransactionRepository,
        )

        launch {
            coroutineContext[Job]?.cancel()
            registrar.updateTxId("draft-id", "tx-hash")
        }
        advanceUntilIdle()

        coVerify { repository.updateTxId("draft-id", "tx-hash") }
        coVerify { locallyCreatedTransactionRepository.markCreated(entity, "tx-hash") }
    }

    private fun pendingTransactionEntity() = PendingTransactionEntity(
        id = "draft-id",
        walletId = "account-id",
        coinUid = "bitcoin",
        blockchainTypeUid = "bitcoin",
        tokenTypeId = "native",
        meta = null,
        amountAtomic = "1",
        feeAtomic = null,
        sdkBalanceAtCreationAtomic = "10",
        fromAddress = "from",
        toAddress = "to",
        txHash = "tx-hash",
        nonce = null,
        memo = null,
        createdAt = 1_000,
        expiresAt = 2_000,
    )
}
