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
    }

    private var refs: Int = 0
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
        get() = refs > 0

    val inBackground: Boolean
        get() = refs == 0

    override fun onActivityStarted(activity: Activity) {
        if (refs == 0) {
            listeners.forEach { listener ->
                listener.willEnterForeground(activity)
                listener.willEnterForeground()
            }
        }
        refs++
    }

    override fun onActivityStopped(activity: Activity) {
        refs--

        if (refs == 0) {
            //App is in background
            listeners.forEach { listener ->
                listener.didEnterBackground()
            }

        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityResumed(p0: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

}
