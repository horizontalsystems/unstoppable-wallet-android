package cash.p.terminal.premium.domain.usecase

import cash.p.terminal.network.binance.api.BinanceApi
import cash.p.terminal.network.binance.data.TokenBalance
import cash.p.terminal.network.pirate.domain.enity.TrialPremiumResult
import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import cash.p.terminal.premium.data.config.PremiumConfig
import cash.p.terminal.premium.data.dao.DemoPremiumUserDao
import cash.p.terminal.premium.data.repository.PremiumUserRepository
import cash.p.terminal.premium.domain.usecase.CheckAdapterPremiumBalanceUseCase.Result.Insufficient
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.WalletFactory
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.managers.UserManager
import cash.p.terminal.wallet.policy.HardwareWalletTokenPolicy
import cash.p.terminal.premium.data.model.PremiumUser
import cash.p.terminal.premium.domain.TestDispatcherProvider
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class CheckPremiumUseCaseTest {

    @MockK
    private lateinit var premiumUserRepository: PremiumUserRepository

    @MockK
    private lateinit var demoPremiumUserDao: DemoPremiumUserDao

    @MockK
    private lateinit var binanceApi: BinanceApi

    @MockK
    private lateinit var piratePlaceRepository: PiratePlaceRepository

    @MockK
    private lateinit var accountManager: IAccountManager

    @MockK
    private lateinit var checkAdapterPremiumBalanceUseCase: CheckAdapterPremiumBalanceUseCase

    @MockK
    private lateinit var checkTrialPremiumUseCase: CheckTrialPremiumUseCase

    @MockK
    private lateinit var activateTrialPremiumUseCase: ActivateTrialPremiumUseCase

    @MockK
    private lateinit var getBnbAddressUseCase: GetBnbAddressUseCase

    @MockK
    private lateinit var userManager: UserManager

    private lateinit var useCase: CheckPremiumUseCaseImpl

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val testDispatcherProvider = TestDispatcherProvider(dispatcher, testScope)
    private val walletFactory = WalletFactory(object : HardwareWalletTokenPolicy {
        override fun isSupported(blockchainType: BlockchainType, tokenType: TokenType) = true
    })

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `update falls back to remote when adapter insufficient`() = runTest(dispatcher) {
        val account = mnemonicAccount()
        val pirateWallet = wallet(account, contract = PremiumConfig.PIRATE_CONTRACT_ADDRESS)

        stubActiveAccount(account)

        coEvery { premiumUserRepository.getByLevel(1) } returns null
        coEvery { premiumUserRepository.getByLevel(0) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(account) } returns TrialPremiumResult.NeedPremium
        coEvery { activateTrialPremiumUseCase.activateTrialPremium(any()) } returns TrialPremiumResult.DemoNotFound

        val insufficient = Insufficient(
            account = account,
            wallet = pirateWallet,
            address = "0xpirate",
            coinType = PremiumConfig.COIN_TYPE_PIRATE
        )
        coEvery { checkAdapterPremiumBalanceUseCase.invoke() } returns insufficient

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(account, any()) } returns "0xcosanta"
        coEvery { getBnbAddressUseCase.getAddress(account) } returns "0xcosanta"
        coEvery { getBnbAddressUseCase.saveAddress(any(), any()) } returns Unit
        coEvery { getBnbAddressUseCase.deleteBnbAddress(any()) } returns Unit

        coEvery {
            binanceApi.getTokenBalance(PremiumConfig.PIRATE_CONTRACT_ADDRESS, "0xcosanta")
        } returns TokenBalance(BigDecimal.ZERO)
        coEvery {
            binanceApi.getTokenBalance(PremiumConfig.COSANTA_CONTRACT_ADDRESS, "0xcosanta")
        } returns TokenBalance(PremiumConfig.MIN_PREMIUM_AMOUNT_COSANTA.toBigDecimal() + BigDecimal.ONE)

        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected fallback")

        coEvery { demoPremiumUserDao.hasActiveTrialPremium() } returns false

        useCase = CheckPremiumUseCaseImpl(
            premiumUserRepository = premiumUserRepository,
            demoPremiumUserDao = demoPremiumUserDao,
            binanceApi = binanceApi,
            piratePlaceRepository = piratePlaceRepository,
            accountManager = accountManager,
            checkAdapterPremiumBalanceUseCase = checkAdapterPremiumBalanceUseCase,
            checkTrialPremiumUseCase = checkTrialPremiumUseCase,
            activateTrialPremiumUseCase = activateTrialPremiumUseCase,
            getBnbAddressUseCase = getBnbAddressUseCase,
            userManager = userManager,
            dispatcherProvider = testDispatcherProvider
        )

        advanceUntilIdle()

        val result = useCase.update()

        assertEquals(PremiumType.COSA, result)
        assertEquals(PremiumType.COSA, useCase.getPremiumType())
    }

    @Test
    fun `getPremiumType prefers cached premium`() = runTest(dispatcher) {
        val account = mnemonicAccount()

        stubActiveAccount(account)

        val cachedUser = PremiumUser(
            level = 1,
            accountId = account.id,
            address = "0xcached",
            lastCheckDate = System.currentTimeMillis(),
            coinType = PremiumConfig.COIN_TYPE_PIRATE,
            isPremium = PremiumType.PIRATE
        )
        coEvery { premiumUserRepository.getByLevel(1) } returns cachedUser
        coEvery { premiumUserRepository.getByLevel(0) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(account) } returns TrialPremiumResult.NeedPremium
        coEvery { activateTrialPremiumUseCase.activateTrialPremium(any()) } returns TrialPremiumResult.DemoNotFound

        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(account, any()) } returns "0xcached"
        coEvery { getBnbAddressUseCase.getAddress(account) } returns "0xcached"
        coEvery { getBnbAddressUseCase.saveAddress(any(), any()) } returns Unit
        coEvery { getBnbAddressUseCase.deleteBnbAddress(any()) } returns Unit

        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected pirate call")

        useCase = createUseCase()

        advanceUntilIdle()

        val result = useCase.getPremiumType()

        assertEquals(PremiumType.PIRATE, result)
        // Adapter is called for parent level (0) which has no cache, but current level (1) uses cache
    }

    @Test
    fun `getPremiumType fetches adapter premium when cache empty`() = runTest(dispatcher) {
        val account = mnemonicAccount()
        val cosantaWallet = wallet(account, PremiumConfig.COSANTA_CONTRACT_ADDRESS)

        stubActiveAccount(account)

        coEvery { premiumUserRepository.getByLevel(1) } returns null
        coEvery { premiumUserRepository.getByLevel(0) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(account) } returns TrialPremiumResult.NeedPremium
        coEvery { activateTrialPremiumUseCase.activateTrialPremium(any()) } returns TrialPremiumResult.DemoNotFound

        coEvery { premiumUserRepository.getByLevel(1) } returns null
        coEvery { premiumUserRepository.getByLevel(0) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(account) } returns TrialPremiumResult.NeedPremium
        coEvery { activateTrialPremiumUseCase.activateTrialPremium(any()) } returns TrialPremiumResult.DemoNotFound

        val premiumResult = CheckAdapterPremiumBalanceUseCase.Result.Premium(
            account = account,
            wallet = cosantaWallet,
            address = "0xcosanta",
            coinType = PremiumConfig.COIN_TYPE_COSANTA,
            premiumType = PremiumType.COSA
        )
        every { checkAdapterPremiumBalanceUseCase.invoke() } returnsMany listOf(null, premiumResult, premiumResult)

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(account, any()) } returns "0xcached"
        coEvery { getBnbAddressUseCase.getAddress(account) } returns "0xcached"
        coEvery { getBnbAddressUseCase.saveAddress(any(), any()) } returns Unit
        coEvery { getBnbAddressUseCase.deleteBnbAddress(any()) } returns Unit

        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected pirate call")

        useCase = createUseCase()

        advanceUntilIdle()

        val premium = useCase.getPremiumType()
        advanceUntilIdle()
        val cached = useCase.getPremiumType()

        assertEquals(PremiumType.COSA, premium)
        assertEquals(PremiumType.COSA, cached)

        verify(atLeast = 2) { checkAdapterPremiumBalanceUseCase.invoke() }
        coVerify { premiumUserRepository.insert(match { it.isPremium == PremiumType.COSA }) }
    }

    @Test
    fun `getPremiumType remains none when adapter reports insufficient`() = runTest(dispatcher) {
        val account = mnemonicAccount()
        val pirateWallet = wallet(account, PremiumConfig.PIRATE_CONTRACT_ADDRESS)

        stubActiveAccount(account)

        coEvery { premiumUserRepository.getByLevel(1) } returns null
        coEvery { premiumUserRepository.getByLevel(0) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(account) } returns TrialPremiumResult.NeedPremium
        coEvery { activateTrialPremiumUseCase.activateTrialPremium(any()) } returns TrialPremiumResult.DemoNotFound

        val insufficient = Insufficient(
            account = account,
            wallet = pirateWallet,
            address = "0xpirate",
            coinType = PremiumConfig.COIN_TYPE_PIRATE
        )
        every { checkAdapterPremiumBalanceUseCase.invoke() } returnsMany listOf(null, insufficient)

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(account, any()) } returns "0xcached"
        coEvery { getBnbAddressUseCase.getAddress(account) } returns "0xcached"
        coEvery { getBnbAddressUseCase.saveAddress(any(), any()) } returns Unit
        coEvery { getBnbAddressUseCase.deleteBnbAddress(any()) } returns Unit

        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected pirate call")

        useCase = createUseCase()

        advanceUntilIdle()

        val result = useCase.getPremiumType()

        assertEquals(PremiumType.NONE, result)
        verify(atLeast = 2) { checkAdapterPremiumBalanceUseCase.invoke() }
    }

    @Test
    fun `getPremiumType remains none when adapter has no data`() = runTest(dispatcher) {
        val account = mnemonicAccount()

        stubActiveAccount(account)

        coEvery { premiumUserRepository.getByLevel(1) } returns null
        coEvery { premiumUserRepository.getByLevel(0) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(account) } returns TrialPremiumResult.NeedPremium
        coEvery { activateTrialPremiumUseCase.activateTrialPremium(any()) } returns TrialPremiumResult.DemoNotFound

        every { checkAdapterPremiumBalanceUseCase.invoke() } returnsMany listOf(null, null)

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(account, any()) } returns "0xcached"
        coEvery { getBnbAddressUseCase.getAddress(account) } returns "0xcached"
        coEvery { getBnbAddressUseCase.saveAddress(any(), any()) } returns Unit
        coEvery { getBnbAddressUseCase.deleteBnbAddress(any()) } returns Unit

        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected pirate call")

        useCase = createUseCase()

        advanceUntilIdle()

        val result = useCase.getPremiumType()

        assertEquals(PremiumType.NONE, result)
        verify(atLeast = 2) { checkAdapterPremiumBalanceUseCase.invoke() }
    }

    // ==================== getParentPremiumType tests ====================

    @Test
    fun `getParentPremiumType returns same as getPremiumType when at level 0`() = runTest(dispatcher) {
        val account = mnemonicAccount(id = "main-account", level = 0)

        stubActiveAccount(account, level = 0)

        val cachedUser = PremiumUser(
            level = 0,
            accountId = account.id,
            address = "0xcached",
            lastCheckDate = System.currentTimeMillis(),
            coinType = PremiumConfig.COIN_TYPE_PIRATE,
            isPremium = PremiumType.PIRATE
        )
        coEvery { premiumUserRepository.getByLevel(0) } returns cachedUser
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(account) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(account) } returns "0xcached"
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected")

        useCase = createUseCase()
        advanceUntilIdle()

        // At level 0, parent level equals current level
        assertEquals(PremiumType.PIRATE, useCase.getPremiumType())
        assertEquals(PremiumType.PIRATE, useCase.getParentPremiumType(userLevel = 0))
    }

    @Test
    fun `getParentPremiumType returns parent cached premium when in duress mode`() = runTest(dispatcher) {
        val mainAccount = mnemonicAccount(id = "main-account", level = 0)
        val duressAccount = mnemonicAccount(id = "duress-account", level = 1)

        stubTwoLevelAccounts(mainAccount, duressAccount, currentLevel = 1)

        // Parent (level 0) has PIRATE premium
        val parentCachedUser = PremiumUser(
            level = 0,
            accountId = mainAccount.id,
            address = "0xparent",
            lastCheckDate = System.currentTimeMillis(),
            coinType = PremiumConfig.COIN_TYPE_PIRATE,
            isPremium = PremiumType.PIRATE
        )
        // Current (level 1) has no premium
        coEvery { premiumUserRepository.getByLevel(0) } returns parentCachedUser
        coEvery { premiumUserRepository.getByLevel(1) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(any()) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(any<Account>()) } returns "0xaddress"
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected")

        useCase = createUseCase()
        advanceUntilIdle()

        assertEquals(PremiumType.NONE, useCase.getPremiumType())
        assertEquals(PremiumType.PIRATE, useCase.getParentPremiumType(0))
    }

    @Test
    fun `getParentPremiumType returns trial when parent has trial premium`() = runTest(dispatcher) {
        val mainAccount = mnemonicAccount(id = "main-account", level = 0)
        val duressAccount = mnemonicAccount(id = "duress-account", level = 1)

        stubTwoLevelAccounts(mainAccount, duressAccount, currentLevel = 1)

        // Parent (level 0) stored in repository - needed for _levelAccountCache
        val parentCachedUser = PremiumUser(
            level = 0,
            accountId = mainAccount.id,
            address = "0xparent",
            lastCheckDate = System.currentTimeMillis(),
            coinType = PremiumConfig.COIN_TYPE_PIRATE,
            isPremium = PremiumType.NONE
        )
        coEvery { premiumUserRepository.getByLevel(0) } returns parentCachedUser
        coEvery { premiumUserRepository.getByLevel(1) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        // Parent account has trial premium active
        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(mainAccount) } returns TrialPremiumResult.DemoActive(daysLeft = 7)
        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(duressAccount) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(any<Account>()) } returns "0xaddress"
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected")

        useCase = createUseCase()
        advanceUntilIdle()

        assertEquals(PremiumType.TRIAL, useCase.getParentPremiumType(0))
    }

    @Test
    fun `getParentPremiumType returns different type than current level`() = runTest(dispatcher) {
        val mainAccount = mnemonicAccount(id = "main-account", level = 0)
        val duressAccount = mnemonicAccount(id = "duress-account", level = 1)

        stubTwoLevelAccounts(mainAccount, duressAccount, currentLevel = 1)

        // Parent (level 0) has COSA premium
        val parentCachedUser = PremiumUser(
            level = 0,
            accountId = mainAccount.id,
            address = "0xparent",
            lastCheckDate = System.currentTimeMillis(),
            coinType = PremiumConfig.COIN_TYPE_COSANTA,
            isPremium = PremiumType.COSA
        )
        // Current (level 1) has PIRATE premium
        val currentCachedUser = PremiumUser(
            level = 1,
            accountId = duressAccount.id,
            address = "0xcurrent",
            lastCheckDate = System.currentTimeMillis(),
            coinType = PremiumConfig.COIN_TYPE_PIRATE,
            isPremium = PremiumType.PIRATE
        )
        coEvery { premiumUserRepository.getByLevel(0) } returns parentCachedUser
        coEvery { premiumUserRepository.getByLevel(1) } returns currentCachedUser
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(any()) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(any<Account>()) } returns "0xaddress"
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected")

        useCase = createUseCase()
        advanceUntilIdle()

        assertEquals(PremiumType.PIRATE, useCase.getPremiumType())
        assertEquals(PremiumType.COSA, useCase.getParentPremiumType(0))
    }

    // ==================== update() tests for parent level ====================

    @Test
    fun `update updates both current and parent levels`() = runTest(dispatcher) {
        val mainAccount = mnemonicAccount(id = "main-account", level = 0)
        val duressAccount = mnemonicAccount(id = "duress-account", level = 1)

        stubTwoLevelAccounts(mainAccount, duressAccount, currentLevel = 1)

        coEvery { premiumUserRepository.getByLevel(0) } returns null
        coEvery { premiumUserRepository.getByLevel(1) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(any()) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(any<Account>()) } returns "0xaddress"
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected")

        useCase = createUseCase()
        advanceUntilIdle()

        useCase.update()

        // Verify both levels were queried
        coVerify { premiumUserRepository.getByLevel(1) }
        coVerify { premiumUserRepository.getByLevel(0) }
    }

    // ==================== isPremiumWithParentInCache tests ====================

    @Test
    fun `isPremiumWithParentInCache returns true when token premium cached for current level`() = runTest(dispatcher) {
        val account = mnemonicAccount()

        stubActiveAccount(account)

        val cachedUser = PremiumUser(
            level = 1,
            accountId = account.id,
            address = "0xcached",
            lastCheckDate = System.currentTimeMillis(),
            coinType = PremiumConfig.COIN_TYPE_PIRATE,
            isPremium = PremiumType.PIRATE
        )
        coEvery { premiumUserRepository.getByLevels(listOf(1, 0)) } returns listOf(cachedUser)
        coEvery { premiumUserRepository.getByLevel(any()) } returns cachedUser
        coEvery { premiumUserRepository.insert(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(any()) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(any<Account>()) } returns "0xcached"
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected")

        useCase = createUseCase()
        advanceUntilIdle()

        val result = useCase.getParentPremiumType(userLevel = 0)

        assertEquals(PremiumType.PIRATE, result)
    }

    @Test
    fun `isPremiumWithParentInCache returns true when token premium cached for parent level`() = runTest(dispatcher) {
        val mainAccount = mnemonicAccount(id = "main-account", level = 0)
        val duressAccount = mnemonicAccount(id = "duress-account", level = 1)

        stubTwoLevelAccounts(mainAccount, duressAccount, currentLevel = 1)

        // Parent (level 0) has PIRATE premium, current (level 1) has none
        val parentCachedUser = PremiumUser(
            level = 0,
            accountId = mainAccount.id,
            address = "0xparent",
            lastCheckDate = System.currentTimeMillis(),
            coinType = PremiumConfig.COIN_TYPE_PIRATE,
            isPremium = PremiumType.PIRATE
        )
        coEvery { premiumUserRepository.getByLevels(listOf(1, 0)) } returns listOf(parentCachedUser)
        coEvery { premiumUserRepository.getByLevel(0) } returns parentCachedUser
        coEvery { premiumUserRepository.getByLevel(1) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(any()) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(any<Account>()) } returns "0xaddress"
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected")

        useCase = createUseCase()
        advanceUntilIdle()

        val result = useCase.getParentPremiumType(userLevel = 0)

        assertEquals(PremiumType.PIRATE, result)
    }

    @Test
    fun `isPremiumWithParentInCache returns true when trial premium is active`() = runTest(dispatcher) {
        val account = mnemonicAccount()

        stubActiveAccount(account)

        // No token premium cached
        coEvery { premiumUserRepository.getByLevels(listOf(0, 0)) } returns emptyList()
        coEvery { premiumUserRepository.getByLevel(any()) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(any()) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(any<Account>()) } returns "0xaddress"
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected")

        // Trial premium IS active in the database
        coEvery { demoPremiumUserDao.hasActiveTrialPremium() } returns true

        useCase = CheckPremiumUseCaseImpl(
            premiumUserRepository = premiumUserRepository,
            demoPremiumUserDao = demoPremiumUserDao,
            binanceApi = binanceApi,
            piratePlaceRepository = piratePlaceRepository,
            accountManager = accountManager,
            checkAdapterPremiumBalanceUseCase = checkAdapterPremiumBalanceUseCase,
            checkTrialPremiumUseCase = checkTrialPremiumUseCase,
            activateTrialPremiumUseCase = activateTrialPremiumUseCase,
            getBnbAddressUseCase = getBnbAddressUseCase,
            userManager = userManager,
            dispatcherProvider = testDispatcherProvider
        )
        advanceUntilIdle()

        val result = useCase.isPremiumWithParentInCache(userLevel = 0)

        assertTrue(result)
    }

    @Test
    fun `isPremiumWithParentInCache returns false when no premium cached`() = runTest(dispatcher) {
        val account = mnemonicAccount()

        stubActiveAccount(account)

        // No token premium cached
        coEvery { premiumUserRepository.getByLevels(listOf(1, 0)) } returns emptyList()
        coEvery { premiumUserRepository.getByLevel(any()) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(any()) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(any<Account>()) } returns "0xaddress"
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected")

        // No trial premium either
        coEvery { demoPremiumUserDao.hasActiveTrialPremium() } returns false

        useCase = createUseCase()
        advanceUntilIdle()

        val result = useCase.getParentPremiumType(userLevel = 0)

        assertEquals(PremiumType.NONE, result)
    }

    @Test
    fun `isPremiumWithParentInCache ignores NONE premium type in cache`() = runTest(dispatcher) {
        val account = mnemonicAccount()

        stubActiveAccount(account)

        // Cached user has NONE premium
        val cachedUser = PremiumUser(
            level = 1,
            accountId = account.id,
            address = "0xcached",
            lastCheckDate = System.currentTimeMillis(),
            coinType = PremiumConfig.COIN_TYPE_PIRATE,
            isPremium = PremiumType.NONE
        )
        coEvery { premiumUserRepository.getByLevels(listOf(1, 0)) } returns listOf(cachedUser)
        coEvery { premiumUserRepository.getByLevel(any()) } returns cachedUser
        coEvery { premiumUserRepository.insert(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(any()) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(any<Account>()) } returns "0xcached"
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected")

        // No trial premium
        coEvery { demoPremiumUserDao.hasActiveTrialPremium() } returns false

        useCase = createUseCase()
        advanceUntilIdle()

        val result = useCase.getParentPremiumType(userLevel = 0)

        assertEquals(PremiumType.NONE, result)
    }

    @Test
    fun update_offlineWithStaleCachedPremium_returnsCachedPremiumNotNone() = runTest(dispatcher) {
        val account = mnemonicAccount()

        stubActiveAccount(account)

        // Stale lastCheckDate forces the full balance-check path (skips checkCachedPremiumStatus).
        val staleCachedUser = PremiumUser(
            level = 1,
            accountId = account.id,
            address = "0xcached",
            lastCheckDate = System.currentTimeMillis() - PremiumConfig.PREMIUM_CHECK_INTERVAL - 1000,
            coinType = PremiumConfig.COIN_TYPE_PIRATE,
            isPremium = PremiumType.PIRATE
        )
        coEvery { premiumUserRepository.getByLevel(1) } returns staleCachedUser
        coEvery { premiumUserRepository.getByLevel(0) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(account) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(account, any()) } returns "0xcached"
        coEvery { getBnbAddressUseCase.getAddress(account) } returns "0xcached"

        // Simulate offline: both balance providers fail to deliver a value.
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns null
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Offline")

        coEvery { demoPremiumUserDao.hasActiveTrialPremium() } returns false

        useCase = createUseCase()
        advanceUntilIdle()

        val result = useCase.update()

        // Stale cache + unreachable balance providers must preserve the last confirmed
        // premium rather than collapse to NONE. Otherwise downstream guards (calculator
        // mode disable) would treat transient network failures as entitlement loss.
        assertEquals(PremiumType.PIRATE, result)
    }

    @Test
    fun update_offlineWithActiveTrialCache_returnsTrialNotNone() = runTest(dispatcher) {
        val account = mnemonicAccount()

        stubActiveAccount(account)

        coEvery { premiumUserRepository.getByLevel(any()) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        // Trial use case keeps returning DemoActive offline thanks to its own Room cache.
        coEvery {
            checkTrialPremiumUseCase.checkTrialPremiumStatus(account)
        } returns TrialPremiumResult.DemoActive(daysLeft = 3)
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(account, any()) } returns "0xaddress"
        coEvery { getBnbAddressUseCase.getAddress(account) } returns "0xaddress"

        coEvery { binanceApi.getTokenBalance(any(), any()) } returns null
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Offline")

        coEvery { demoPremiumUserDao.hasActiveTrialPremium() } returns true

        useCase = createUseCase()
        advanceUntilIdle()

        val result = useCase.update()

        // Trial users must remain TRIAL when balance providers are unreachable, not be
        // demoted to NONE.
        assertEquals(PremiumType.TRIAL, result)
    }

    @Test
    fun `update does not update parent level when at level 0`() = runTest(dispatcher) {
        val account = mnemonicAccount(id = "main-account", level = 0)

        stubActiveAccount(account, level = 0)

        coEvery { premiumUserRepository.getByLevel(0) } returns null
        coEvery { premiumUserRepository.insert(any()) } returns Unit
        coEvery { premiumUserRepository.deleteByAccount(any()) } returns Unit

        coEvery { checkTrialPremiumUseCase.checkTrialPremiumStatus(account) } returns TrialPremiumResult.NeedPremium
        every { checkAdapterPremiumBalanceUseCase.invoke() } returns null

        coEvery { getBnbAddressUseCase.deleteExcludeAccountIds(any()) } returns Unit
        coEvery { getBnbAddressUseCase.getAddress(account) } returns "0xaddress"
        coEvery { binanceApi.getTokenBalance(any(), any()) } returns TokenBalance(BigDecimal.ZERO)
        coEvery { piratePlaceRepository.getInvestmentData(any(), any()) } throws IllegalStateException("Unexpected")

        useCase = createUseCase()
        advanceUntilIdle()

        useCase.update()

        // At level 0, parent == current, so getByLevel(0) called but not getByLevel(-1)
        coVerify(atLeast = 1) { premiumUserRepository.getByLevel(0) }
        coVerify(exactly = 0) { premiumUserRepository.getByLevel(-1) }
    }

    private fun createUseCase(): CheckPremiumUseCaseImpl {
        coEvery { demoPremiumUserDao.hasActiveTrialPremium() } returns false
        return CheckPremiumUseCaseImpl(
            premiumUserRepository = premiumUserRepository,
            demoPremiumUserDao = demoPremiumUserDao,
            binanceApi = binanceApi,
            piratePlaceRepository = piratePlaceRepository,
            accountManager = accountManager,
            checkAdapterPremiumBalanceUseCase = checkAdapterPremiumBalanceUseCase,
            checkTrialPremiumUseCase = checkTrialPremiumUseCase,
            activateTrialPremiumUseCase = activateTrialPremiumUseCase,
            getBnbAddressUseCase = getBnbAddressUseCase,
            userManager = userManager,
            dispatcherProvider = testDispatcherProvider
        )
    }

    private fun stubActiveAccount(account: Account, level: Int = 1) {
        val levelFlow = MutableStateFlow(level)
        every { userManager.currentUserLevelFlow } returns levelFlow
        val accountsFlow = MutableStateFlow(listOf(account))
        every { accountManager.accountsFlow } returns accountsFlow
        every { accountManager.accounts } returns listOf(account)
        every { accountManager.activeAccount } returns account
        every { accountManager.account(account.id) } returns account
    }

    private fun stubTwoLevelAccounts(
        mainAccount: Account,
        duressAccount: Account,
        currentLevel: Int
    ) {
        val levelFlow = MutableStateFlow(currentLevel)
        every { userManager.currentUserLevelFlow } returns levelFlow
        val allAccounts = listOf(mainAccount, duressAccount)
        val accountsFlow = MutableStateFlow(allAccounts)
        every { accountManager.accountsFlow } returns accountsFlow
        every { accountManager.accounts } returns allAccounts
        every { accountManager.activeAccount } returns if (currentLevel == 0) mainAccount else duressAccount
        every { accountManager.account(mainAccount.id) } returns mainAccount
        every { accountManager.account(duressAccount.id) } returns duressAccount
    }

    private fun mnemonicAccount(id: String = "account-id", level: Int = 1): Account = Account(
        id = id,
        name = "Account",
        type = AccountType.Mnemonic(
            words = List(12) { "abandon" },
            passphrase = ""
        ),
        origin = AccountOrigin.Created,
        level = level,
        isBackedUp = true
    )

    private fun wallet(account: Account, contract: String): Wallet = walletFactory.create(
        token = Token(
            coin = Coin(uid = contract, name = "Token", code = "TKN"),
            blockchain = Blockchain(BlockchainType.BinanceSmartChain, "BSC", null),
            type = TokenType.Eip20(contract),
            decimals = 18
        ),
        account = account,
        hardwarePublicKey = null
    )!!
}
