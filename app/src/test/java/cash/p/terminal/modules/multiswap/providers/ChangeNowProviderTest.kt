package cash.p.terminal.modules.multiswap.providers

import cash.p.terminal.core.storage.SwapProviderTransactionsStorage
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.network.changenow.domain.repository.ChangeNowRepository
import cash.p.terminal.network.pirate.domain.useCase.GetChangeNowAssociatedCoinTickerUseCase
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.useCases.WalletUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ChangeNowProviderTest {

    private val walletUseCase = mockk<WalletUseCase>(relaxed = true)
    private val changeNowRepository = mockk<ChangeNowRepository>(relaxed = true)
    private val getTickerUseCase = mockk<GetChangeNowAssociatedCoinTickerUseCase>(relaxed = true)
    private val storage = mockk<SwapProviderTransactionsStorage>(relaxed = true)
    private val accountManager = mockk<IAccountManager>(relaxed = true)
    private val marketKit = mockk<MarketKitWrapper>(relaxed = true)

    @Before
    fun setUp() {
        every { accountManager.activeAccount } returns buildTestAccount("acc-1")
        startKoin {
            modules(module { single { marketKit } })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun onTransactionCompleted_savesTransactionWithRecordUidAndUpdatedDate() {
        val provider = createProvider()
        val transaction = buildSwapProviderTransaction(SwapProvider.CHANGENOW, "tx-123")
        val result = mockk<SendTransactionResult>(relaxed = true)
        every { result.getRecordUid() } returns "record-uid-99"

        provider.onTransactionCompleted(transaction, result)

        verify {
            storage.save(
                match {
                    it.transactionId == "tx-123" &&
                        it.outgoingRecordUid == "record-uid-99" &&
                        it.date >= transaction.date
                }
            )
        }
    }

    @Test
    fun activeAccountChanged_invalidatesZcashTransparentAddressCache() = runTest {
        val transparentToken = mockZcashToken(TokenType.AddressSpecType.Transparent)
        marketKit.stubZcashTransparentToken(transparentToken)
        coEvery { walletUseCase.getOneTimeReceiveAddress(transparentToken) } returnsMany
            listOf("address-acc-1", "address-acc-2")

        val zcashUnifiedToken = mockZcashToken(TokenType.AddressSpecType.Unified)
        val anyToken = mockk<Token>(relaxed = true)

        val provider = createProvider()

        val before = provider.getWarningMessage(zcashUnifiedToken, anyToken)
        assertEquals("address-acc-1", before.formatArgFirst())

        every { accountManager.activeAccount } returns buildTestAccount("acc-2")

        val after = provider.getWarningMessage(zcashUnifiedToken, anyToken)
        assertEquals("address-acc-2", after.formatArgFirst())
    }

    @Test
    fun sameAccount_reusesZcashTransparentAddressCache() = runTest {
        val transparentToken = mockZcashToken(TokenType.AddressSpecType.Transparent)
        marketKit.stubZcashTransparentToken(transparentToken)
        coEvery { walletUseCase.getOneTimeReceiveAddress(transparentToken) } returns "addr-cached"

        val zcashUnifiedToken = mockZcashToken(TokenType.AddressSpecType.Unified)
        val anyToken = mockk<Token>(relaxed = true)

        val provider = createProvider()

        repeat(3) { provider.getWarningMessage(zcashUnifiedToken, anyToken) }

        coEvery { walletUseCase.getOneTimeReceiveAddress(transparentToken) } returns "addr-fresh"

        val cached = provider.getWarningMessage(zcashUnifiedToken, anyToken)
        assertEquals("addr-cached", cached.formatArgFirst())
    }

    @Test
    fun getWarningMessage_nonZcashTokenIn_returnsNull() = runTest {
        val provider = createProvider()

        assertNull(provider.getWarningMessage(mockNonZcashNativeToken(), mockk(relaxed = true)))
    }

    private fun createProvider() = ChangeNowProvider(
        walletUseCase = walletUseCase,
        changeNowRepository = changeNowRepository,
        getChangeNowAssociatedCoinTickerUseCase = getTickerUseCase,
        accountManager = accountManager,
        providerSupport = buildOffChainSwapProviderSupport(
            walletUseCase = walletUseCase,
            accountManager = accountManager,
            storage = storage,
            marketKit = marketKit,
        ),
    )
}
