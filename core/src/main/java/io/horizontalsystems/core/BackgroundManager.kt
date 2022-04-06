package io.horizontalsystems.core

import android.app.Activity
import android.app.Application
import android.os.Bundle

class BackgroundManager(application: Application) : Application.ActivityLifecycleCallbacks {

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    interface Listener {
        fun willEnterForeground(activity: Activity) {}
        fun willEnterForeground() {}
        fun didEnterBackground() {}
        fun onAllActivitiesDestroyed() {}
    }

    private var foregroundActivityCount: Int = 0
    private var aliveActivityCount: Int = 0
    private var listeners: MutableList<Listener> = ArrayList()

    @Synchronized
    fun registerListener(listener: Listener) {
        listeners.add(listener)
    }

    @Synchronized
    fun unregisterListener(listener: Listener) {
        listeners.remove(listener)
    }

    val inForeground: Boolean
        get() = foregroundActivityCount > 0

    val inBackground: Boolean
        get() = foregroundActivityCount == 0

    override fun onActivityStarted(activity: Activity) {
        if (foregroundActivityCount == 0) {
            listeners.forEach { listener ->
                listener.willEnterForeground(activity)
                listener.willEnterForeground()
            }
        }
        foregroundActivityCount++
    }

    override fun onActivityStopped(activity: Activity) {
        foregroundActivityCount--

        if (foregroundActivityCount == 0) {
            //App is in background
            listeners.forEach { listener ->
                listener.didEnterBackground()
            }

        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        aliveActivityCount++
    }

    override fun onActivityDestroyed(activity: Activity) {
        aliveActivityCount--

        if (aliveActivityCount == 0) {
            listeners.forEach { listener ->
                listener.onAllActivitiesDestroyed()
            }
        }
    }

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityResumed(p0: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

}
