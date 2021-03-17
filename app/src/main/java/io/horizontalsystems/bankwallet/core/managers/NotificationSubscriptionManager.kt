package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import io.horizontalsystems.bankwallet.core.INotificationSubscriptionManager
import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.entities.SubscriptionJob
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers

class NotificationSubscriptionManager(appDatabase: AppDatabase): INotificationSubscriptionManager {

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
            SubscriptionJob.JobType.Subscribe -> subscribe(subscriptionJob.topicName)
            else -> unsubscribe(subscriptionJob.topicName)
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

     private fun subscribe(topicName: String): Flowable<Unit> {
        return Flowable.create(({ emitter ->
            FirebaseMessaging.getInstance().subscribeToTopic(topicName)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful){
                            emitter.onNext(Unit)
                        } else {
                            emitter.onError(task.exception ?: Throwable("Error in subscribing to  notification topic"))
                        }
                        emitter.onComplete()
                    }
        }), BackpressureStrategy.BUFFER)
    }

     private fun unsubscribe(topicName: String): Flowable<Unit> {
        return Flowable.create(({ emitter ->
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topicName)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful){
                            emitter.onNext(Unit)
                        } else {
                            emitter.onError(task.exception ?: Throwable("Error in unsubscribing to  notification topic"))
                        }
                        emitter.onComplete()
                    }
        }), BackpressureStrategy.BUFFER)
    }
}
