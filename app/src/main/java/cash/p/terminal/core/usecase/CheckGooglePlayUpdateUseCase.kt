package cash.p.terminal.core.usecase

import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

class CheckGooglePlayUpdateUseCase {
    private val appUpdateManager: AppUpdateManager by inject(AppUpdateManager::class.java)

    operator fun invoke(): Flow<UpdateResult> = flow {
        try {
            val appUpdateInfo = withContext(Dispatchers.IO) {
                Tasks.await(appUpdateManager.appUpdateInfo)
            }

            emit(
                when {
                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        when {
                            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> UpdateResult.ImmediateUpdateAvailable
                            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> UpdateResult.FlexibleUpdateAvailable
                            else -> UpdateResult.NoUpdate
                        }
                    }

                    else -> UpdateResult.NoUpdate
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emit(UpdateResult.Error)
        }
    }
}

sealed class UpdateResult {
    data object NoUpdate : UpdateResult()
    data object ImmediateUpdateAvailable : UpdateResult()
    data object FlexibleUpdateAvailable : UpdateResult()
    data object Error : UpdateResult()
}