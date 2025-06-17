package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import com.tangem.operations.backup.BackupService
import com.tangem.operations.backup.PrimaryCard
import com.tangem.sdk.extensions.init
import io.horizontalsystems.core.BackgroundManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class BackupHardwareWalletUseCase(
    private val backgroundManager: BackgroundManager,
    private val tangemSdkManager: TangemSdkManager
) {
    val addedBackupCardsCount: Int
        get() = backupService.addedBackupCardsCount

    private val backupService: BackupService by lazy {
        BackupService.init(tangemSdkManager.tangemSdk, backgroundManager.currentActivity!!)
    }

    suspend fun addBackup(primaryCard: PrimaryCard) = suspendCoroutine { continuation ->
        if ( backupService.backupCardIds.isEmpty() && backupService.primaryCardId != null) {
            backupService.discardSavedBackup()
        }
        backupService.setPrimaryCard(primaryCard)
        backupService.addBackupCard {
            continuation.resume(it)
        }
    }

    fun setAccessCode(accessCode: String) {
        backupService.setAccessCode(accessCode)
    }

    suspend fun proceedBackup() = suspendCoroutine { continuation ->
        backupService.proceedBackup {
            continuation.resume(it)
        }
    }

    fun isBackupFinished() = backupService.currentState == BackupService.State.Finished
}