package cash.p.terminal.premium.domain.usecase

import cash.p.terminal.network.pirate.domain.enity.TrialPremiumResult
import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import cash.p.terminal.premium.data.dao.DemoPremiumUserDao
import cash.p.terminal.premium.data.model.DemoPremiumUser
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.eligibleForPremium
import timber.log.Timber

internal class CheckTrialPremiumUseCase(
    private val piratePlaceRepository: PiratePlaceRepository,
    private val demoPremiumUserDao: DemoPremiumUserDao,
    private val getBnbAddressUseCase: GetBnbAddressUseCaseImpl,
    private val isDebug: Boolean
) {

    suspend fun checkTrialPremiumStatus(account: Account): TrialPremiumResult {
        if (!account.eligibleForPremium()) {
            return TrialPremiumResult.DemoNotFound
        }

        var cachedUser: DemoPremiumUser? = null
        return try {
            val walletAddress = getBnbAddressUseCase.getAddress(account)
                ?: throw IllegalStateException("Wallet address not found")

            cachedUser = findCachedUser(walletAddress) ?: return TrialPremiumResult.NeedPremium

            // Debug builds skip the local countdown and always ask the server, so testers
            // see server-side status changes immediately. Release builds trust the local
            // countdown and only hit the network once it has run out.
            if (!isDebug) {
                resolveFromCache(cachedUser)?.let { return it }
            }

            // Check network for updated status
            val premiumStatus = piratePlaceRepository.checkTrialPremiumStatus(walletAddress)
            when (premiumStatus) {
                is TrialPremiumResult.DemoNotFound,
                is TrialPremiumResult.DemoExpired -> {
                    // Update cache with expired status
                    demoPremiumUserDao.deleteByAddress(walletAddress)
                }

                is TrialPremiumResult.DemoActive -> {
                    // Update cache with new active status
                    demoPremiumUserDao.insert(
                        cachedUser.copy(
                            daysLeft = premiumStatus.daysLeft,
                            lastCheckDate = System.currentTimeMillis()
                        )
                    )
                }

                else -> Unit
            }
            premiumStatus ?: TrialPremiumResult.DemoActive(daysLeft = cachedUser.daysLeft)
        } catch (e: Exception) {
            Timber.d(e, "Error checking premium status for wallet ${cachedUser?.address}")
            TrialPremiumResult.DemoError()
        }
    }

    private suspend fun findCachedUser(walletAddress: String): DemoPremiumUser? {
        return demoPremiumUserDao.getByAddress(walletAddress)
    }

    /**
     * Resolves the trial status from the local countdown without hitting the network.
     * Returns null when the cache can no longer be trusted and the server must be queried.
     */
    private fun resolveFromCache(cachedUser: DemoPremiumUser): TrialPremiumResult? {
        val daysPassed = (System.currentTimeMillis() - cachedUser.lastCheckDate) / (24 * 60 * 60 * 1000)
        val remainingDays = cachedUser.daysLeft - daysPassed.toInt()
        return when {
            remainingDays > 0 -> TrialPremiumResult.DemoActive(daysLeft = remainingDays)
            cachedUser.daysLeft == 0 -> TrialPremiumResult.DemoExpired
            else -> null
        }
    }
}