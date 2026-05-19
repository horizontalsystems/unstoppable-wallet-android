package cash.p.terminal.core.managers

import cash.p.terminal.core.IRestoreSettingsStorage
import cash.p.terminal.core.usecase.ValidateMoneroHeightUseCase
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class RestoreSettingsManagerTest {

    private val storage = mockk<IRestoreSettingsStorage>(relaxed = true)
    private val zcashBirthdayProvider = mockk<ZcashBirthdayProvider>(relaxed = true)
    private val litecoinBirthdayProvider = mockk<LitecoinBirthdayProvider>()
    private val validateMoneroHeightUseCase = mockk<ValidateMoneroHeightUseCase>(relaxed = true)

    @Test
    fun getSettingValueForCreatedAccount_litecoinBirthdayHeight_returnsLatestCheckpoint() {
        every { litecoinBirthdayProvider.getLatestCheckpointBlockHeight() } returns LITECOIN_CHECKPOINT
        val manager = RestoreSettingsManager(
            storage = storage,
            zcashBirthdayProvider = zcashBirthdayProvider,
            litecoinBirthdayProvider = litecoinBirthdayProvider,
            validateMoneroHeightUseCase = validateMoneroHeightUseCase
        )

        val value = manager.getSettingValueForCreatedAccount(
            RestoreSettingType.BirthdayHeight,
            BlockchainType.Litecoin
        )

        assertEquals(LITECOIN_CHECKPOINT.toString(), value)
    }

    private companion object {
        const val LITECOIN_CHECKPOINT = 3_000_000L
    }
}
