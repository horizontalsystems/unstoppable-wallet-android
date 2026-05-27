package cash.p.terminal.premium.domain.usecase

import cash.p.terminal.network.pirate.domain.enity.TrialPremiumResult
import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import cash.p.terminal.premium.data.dao.DemoPremiumUserDao
import cash.p.terminal.premium.data.model.DemoPremiumUser
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CheckTrialPremiumUseCaseTest {

    @MockK
    private lateinit var piratePlaceRepository: PiratePlaceRepository

    @MockK
    private lateinit var demoPremiumUserDao: DemoPremiumUserDao

    @MockK
    private lateinit var getBnbAddressUseCase: GetBnbAddressUseCaseImpl

    private val dispatcher = StandardTestDispatcher()

    private val account = eligibleAccount()
    private val walletAddress = "0xWALLET"
    private val day = 24L * 60 * 60 * 1000

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        coEvery { getBnbAddressUseCase.getAddress(account) } returns walletAddress
        coEvery { getBnbAddressUseCase.getAddress(account, any()) } returns walletAddress
    }

    @Test
    fun checkTrialPremiumStatus_releaseBuildCounterActive_returnsCacheWithoutNetwork() =
        runTest(dispatcher) {
            stubCachedUser(daysLeft = 30, lastCheckDate = System.currentTimeMillis())

            val result = createUseCase(isDebug = false).checkTrialPremiumStatus(account)

            assertEquals(TrialPremiumResult.DemoActive(daysLeft = 30), result)
            coVerify(exactly = 0) { piratePlaceRepository.checkTrialPremiumStatus(any()) }
        }

    @Test
    fun checkTrialPremiumStatus_releaseBuildZeroDays_returnsExpiredWithoutNetwork() =
        runTest(dispatcher) {
            stubCachedUser(daysLeft = 0, lastCheckDate = System.currentTimeMillis())

            val result = createUseCase(isDebug = false).checkTrialPremiumStatus(account)

            assertEquals(TrialPremiumResult.DemoExpired, result)
            coVerify(exactly = 0) { piratePlaceRepository.checkTrialPremiumStatus(any()) }
        }

    @Test
    fun checkTrialPremiumStatus_releaseBuildCounterElapsed_queriesNetwork() =
        runTest(dispatcher) {
            // daysLeft positive but lastCheckDate old enough that the countdown ran out
            stubCachedUser(daysLeft = 5, lastCheckDate = System.currentTimeMillis() - 10 * day)
            coEvery { piratePlaceRepository.checkTrialPremiumStatus(walletAddress) } returns
                    TrialPremiumResult.DemoActive(daysLeft = 20)

            val result = createUseCase(isDebug = false).checkTrialPremiumStatus(account)

            assertEquals(TrialPremiumResult.DemoActive(daysLeft = 20), result)
            coVerify(exactly = 1) { piratePlaceRepository.checkTrialPremiumStatus(walletAddress) }
        }

    @Test
    fun checkTrialPremiumStatus_debugBuildCounterActive_alwaysQueriesNetwork() =
        runTest(dispatcher) {
            // Counter is still valid; in release it would return cache, in debug it must hit network
            stubCachedUser(daysLeft = 30, lastCheckDate = System.currentTimeMillis())
            coEvery { piratePlaceRepository.checkTrialPremiumStatus(walletAddress) } returns
                    TrialPremiumResult.DemoExpired

            val result = createUseCase(isDebug = true).checkTrialPremiumStatus(account)

            assertEquals(TrialPremiumResult.DemoExpired, result)
            coVerify(exactly = 1) { piratePlaceRepository.checkTrialPremiumStatus(walletAddress) }
        }

    private fun stubCachedUser(daysLeft: Int, lastCheckDate: Long) {
        coEvery { demoPremiumUserDao.getByAddress(walletAddress) } returns DemoPremiumUser(
            address = walletAddress,
            lastCheckDate = lastCheckDate,
            daysLeft = daysLeft
        )
    }

    private fun createUseCase(isDebug: Boolean) = CheckTrialPremiumUseCase(
        piratePlaceRepository = piratePlaceRepository,
        demoPremiumUserDao = demoPremiumUserDao,
        getBnbAddressUseCase = getBnbAddressUseCase,
        isDebug = isDebug
    )

    private fun eligibleAccount(): Account = Account(
        id = "account-id",
        name = "Account",
        type = AccountType.Mnemonic(
            words = List(12) { "abandon" },
            passphrase = ""
        ),
        origin = AccountOrigin.Created,
        level = 0,
        isBackedUp = true
    )
}
