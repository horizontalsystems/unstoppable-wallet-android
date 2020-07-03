package io.horizontalsystems.bankwallet.core.managers

import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.INotificationManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import android.app.NotificationManager as SystemNotificationManager

class NotificationManager(private val androidNotificationManager: NotificationManagerCompat) : INotificationManager {

    private val maxNumberOfNotifications = 2

    override val isEnabled: Boolean
        get() = when {
            !androidNotificationManager.areNotificationsEnabled() -> false
            else -> {
                val notificationChannel = androidNotificationManager.getNotificationChannel(channelId)
                notificationChannel?.importance != NotificationManagerCompat.IMPORTANCE_NONE
            }
        }

    override fun clear() {
        for(i in 0 until maxNumberOfNotifications){
            androidNotificationManager.cancel(i)
        }
    }

    override fun subscribe(topicName: String): Flowable<Unit> {
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

    override fun unsubscribe(topicName: String): Flowable<Unit> {
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

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = App.instance.getString(R.string.App_Name)
            val importance = SystemNotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            // Register the channel with the system
            androidNotificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val channelId = "priceAlert"
    }
}
