package io.horizontalsystems.bankwallet.core.managers

import android.app.Activity
import android.app.Application
import android.os.Bundle
import io.horizontalsystems.bankwallet.core.App

class BackgroundManager(application: Application) : Application.ActivityLifecycleCallbacks {

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    private var refs: Int = 0

    val inForeground: Boolean
        get() = refs > 0

    val inBackground: Boolean
        get() = refs == 0

    override fun onActivityStarted(activity: Activity?) {
        if(refs == 0) {
            App.lockManager.willEnterForeground()
        }
        refs++
    }

    override fun onActivityStopped(activity: Activity?) {
        refs--

        if (refs == 0) {
            //App is in background
            App.lockManager.didEnterBackground()
        }
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}

    override fun onActivityPaused(p0: Activity?) {}

    override fun onActivityResumed(p0: Activity?) {}

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

    override fun onActivityDestroyed(activity: Activity?) {}

}