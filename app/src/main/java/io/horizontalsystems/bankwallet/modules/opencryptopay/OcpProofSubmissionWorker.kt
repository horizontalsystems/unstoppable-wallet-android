package io.horizontalsystems.bankwallet.modules.opencryptopay

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.horizontalsystems.bankwallet.core.App
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit

class OcpProofSubmissionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val txHash = inputData.getString(KEY_TX_HASH) ?: return Result.failure()
        val dao = App.appDatabase.ocpPaymentDao()
        val record = dao.getByTxHash(txHash) ?: return Result.success()
        if (record.proofSubmittedAt != null) return Result.success()

        val baseUrl = record.proofUrl.substringBefore("/tx/").let { it.trimEnd('/') + "/" }
        Timber.d(
            "OCP worker GET /tx/ url=${record.proofUrl} quote=${record.quoteId}" +
            " method=${record.method} tx=${record.txHash} attempt=$runAttemptCount" +
            " expirationIso=${record.expirationIso}"
        )
        return try {
            OcpProofService.service(baseUrl).submitProofTx(
                url = record.proofUrl,
                quote = record.quoteId,
                method = record.method,
                tx = record.txHash,
            )
            Timber.d("OCP worker /tx/ success tx=$txHash")
            dao.markSubmitted(txHash, System.currentTimeMillis())
            Result.success()
        } catch (e: HttpException) {
            val code = e.code()
            val body = e.response()?.errorBody()?.string()
            Timber.e("OCP worker /tx/ HTTP $code body=$body tx=$txHash")
            when {
                code == 429 -> Result.retry()        // rate limited — back off and retry
                code in 400..499 -> {
                    dao.markFailed(txHash, System.currentTimeMillis())
                    Result.failure()                  // permanent client error (expired quote, etc.)
                }
                else -> Result.retry()               // 5xx or other transient
            }
        } catch (e: Exception) {
            Timber.d("OCP worker /tx/ transient failure (${e.javaClass.simpleName}: ${e.message}) tx=$txHash")
            Result.retry()
        }
    }

    companion object {
        const val KEY_TX_HASH = "txHash"

        fun enqueue(context: Context, txHash: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<OcpProofSubmissionWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(Data.Builder().putString(KEY_TX_HASH, txHash).build())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueName(txHash),
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        private fun uniqueName(txHash: String) = "ocp_proof_$txHash"
    }
}
