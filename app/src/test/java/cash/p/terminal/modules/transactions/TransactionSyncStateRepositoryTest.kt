package cash.p.terminal.modules.transactions

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.transaction.TransactionSource
import io.mockk.every
import io.mockk.mockk
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TransactionSyncStateRepositoryTest {
    private val adapterManager: TransactionAdapterManager = mockk(relaxed = true)

    @Test
    fun setTransactionWallets_missingAdapterWhileInitializing_keepsSyncingUntilInitialized() = runBlocking {
        val initializationFlow = MutableStateFlow(false)
        val missingSource = mockk<TransactionSource>()
        val repository = createRepository(initializationFlow)

        every { adapterManager.getAdapter(missingSource) } returns null

        try {
            repository.setTransactionWallets(listOf(TransactionWallet(null, missingSource, null)))

            assertTrue(repository.syncingFlow.value)

            initializationFlow.value = true

            awaitNotSyncing(repository)
        } finally {
            repository.clear()
        }
    }

    @Test
    fun setTransactionWallets_syncedAdapterAndMissingAdapterAfterInitialization_reportsNotSyncing() = runBlocking {
        val initializationFlow = MutableStateFlow(true)
        val syncedSource = mockk<TransactionSource>()
        val missingSource = mockk<TransactionSource>()
        val repository = createRepository(initializationFlow)

        every { adapterManager.getAdapter(syncedSource) } returns adapter(AdapterState.Synced)
        every { adapterManager.getAdapter(missingSource) } returns null

        try {
            repository.setTransactionWallets(
                listOf(
                    TransactionWallet(null, syncedSource, null),
                    TransactionWallet(null, missingSource, null)
                )
            )

            awaitNotSyncing(repository)
        } finally {
            repository.clear()
        }
    }

    @Test
    fun setTransactionWallets_cancelledSetup_doesNotStartOldMonitoring() = runBlocking {
        val initializationFlow = MutableStateFlow(true)
        val oldSource = mockk<TransactionSource>()
        val newSource = mockk<TransactionSource>()
        val oldStateRead = StateReadProbe()
        val oldSubscription = SubscriptionProbe()
        val newSubscription = SubscriptionProbe()
        val repository = createRepository(initializationFlow)

        every { adapterManager.getAdapter(oldSource) } returns adapter(
            stateProvider = oldStateRead::read,
            blockUpdates = oldSubscription.flow
        )
        every { adapterManager.getAdapter(newSource) } returns adapter(
            blockUpdates = newSubscription.flow
        )

        try {
            repository.setTransactionWallets(listOf(TransactionWallet(null, oldSource, null)))
            oldStateRead.awaitStarted()

            repository.setTransactionWallets(listOf(TransactionWallet(null, newSource, null)))
            oldStateRead.release()

            oldStateRead.awaitReturned()
            newSubscription.awaitSubscribed()
            oldSubscription.assertNotSubscribed()
        } finally {
            oldStateRead.release()
            repository.clear()
        }
    }

    private fun createRepository(
        initializationFlow: MutableStateFlow<Boolean>
    ): TransactionSyncStateRepository {
        every { adapterManager.initializationFlow } returns initializationFlow
        return TransactionSyncStateRepository(adapterManager)
    }

    private fun adapter(
        state: AdapterState = AdapterState.Synced,
        txUpdates: Flowable<Unit> = Flowable.never(),
        blockUpdates: Flowable<Unit> = Flowable.never(),
        stateProvider: (() -> AdapterState)? = null
    ): ITransactionsAdapter =
        mockk(relaxed = true) {
            if (stateProvider == null) {
                every { transactionsState } returns state
            } else {
                every { transactionsState } answers { stateProvider() }
            }
            every { transactionsStateUpdatedFlowable } returns txUpdates
            every { lastBlockUpdatedFlowable } returns blockUpdates
        }

    private class StateReadProbe {
        private val started = CountDownLatch(1)
        private val released = CountDownLatch(1)
        private val returned = CountDownLatch(1)

        fun read(): AdapterState {
            started.countDown()
            released.await(1, TimeUnit.SECONDS)
            returned.countDown()
            return AdapterState.Synced
        }

        fun awaitStarted() = assertTrue(started.await(1, TimeUnit.SECONDS))

        fun release() {
            released.countDown()
        }

        fun awaitReturned() = assertTrue(returned.await(1, TimeUnit.SECONDS))
    }

    private class SubscriptionProbe {
        private val subscribed = CountDownLatch(1)

        val flow: Flowable<Unit> = PublishSubject.create<Unit>()
            .toFlowable(BackpressureStrategy.BUFFER)
            .doOnSubscribe { subscribed.countDown() }

        fun awaitSubscribed() = assertTrue(subscribed.await(1, TimeUnit.SECONDS))

        fun assertNotSubscribed() = assertFalse(subscribed.await(300, TimeUnit.MILLISECONDS))
    }

    private suspend fun awaitNotSyncing(repository: TransactionSyncStateRepository) {
        withTimeout(1_000) {
            repository.syncingFlow.first { !it }
        }
        assertFalse(repository.syncingFlow.value)
    }
}
