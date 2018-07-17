package bitcoin.wallet.core.managers

import android.app.Activity
import android.app.Application
import android.os.Bundle
import bitcoin.wallet.core.App

object BackgroundManager : Application.ActivityLifecycleCallbacks {

    private var refs: Int = 0

    val inForeground: Boolean
        get() = refs > 0

    val inBackground: Boolean
        get() = refs == 0

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity?) {
        refs++
    }

    override fun onActivityStopped(activity: Activity?) {
        refs--

        if (refs == 0) {
            //App is in background

            App.promptPin = true
        }
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}

    override fun onActivityPaused(p0: Activity?) {}

    override fun onActivityResumed(p0: Activity?) {}

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

    override fun onActivityDestroyed(activity: Activity?) {}

}