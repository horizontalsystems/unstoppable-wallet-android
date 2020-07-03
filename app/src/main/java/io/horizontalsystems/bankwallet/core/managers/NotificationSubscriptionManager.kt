package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import io.horizontalsystems.bankwallet.core.INotificationManager
import io.horizontalsystems.bankwallet.core.INotificationSubscriptionManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.SubscriptionJob
import io.reactivex.schedulers.Schedulers

class NotificationSubscriptionManager(
        appDatabase: AppDatabase,
        private val notificationManager: INotificationManager): INotificationSubscriptionManager {

    private val dao = appDatabase.subscriptionJobDao()

    override fun processJobs() {
        val jobs = dao.all()
        jobs.forEach {
            processJob(it)
        }
    }

    override fun addNewJobs(jobs: List<SubscriptionJob>) {
        jobs.forEach {
            dao.save(it)
            processJob(it)
        }
    }

    private fun processJob(subscriptionJob: SubscriptionJob) {
        val flowable = when (subscriptionJob.jobType) {
            SubscriptionJob.JobType.Subscribe -> notificationManager.subscribe(subscriptionJob.topicName)
            else -> notificationManager.unsubscribe(subscriptionJob.topicName)
        }

        val disposable = flowable
                .firstOrError()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    dao.delete(subscriptionJob)
                },{
                    Log.e("NotifSubscrManager", "subscribe error", it)
                })
    }
}
