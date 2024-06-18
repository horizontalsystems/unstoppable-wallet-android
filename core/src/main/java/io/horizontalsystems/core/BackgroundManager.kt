package io.horizontalsystems.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class BackgroundManager(application: Application) : Application.ActivityLifecycleCallbacks {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val _stateFlow: MutableSharedFlow<BackgroundManagerState> = MutableSharedFlow()
    val stateFlow: SharedFlow<BackgroundManagerState>
        get() = _stateFlow

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    private var foregroundActivityCount: Int = 0
    private var aliveActivityCount: Int = 0

    @Synchronized
    override fun onActivityStarted(activity: Activity) {
        if (foregroundActivityCount == 0) {
            scope.launch {
                _stateFlow.emit(BackgroundManagerState.EnterForeground)
            }
        }
        foregroundActivityCount++
    }

    @Synchronized
    override fun onActivityStopped(activity: Activity) {
        foregroundActivityCount--

        if (foregroundActivityCount == 0) {
            //App is in background
            scope.launch {
                _stateFlow.emit(BackgroundManagerState.EnterBackground)
            }
        }
    }

    @Synchronized
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        aliveActivityCount++
    }

    @Synchronized
    override fun onActivityDestroyed(activity: Activity) {
        aliveActivityCount--

        if (aliveActivityCount == 0) {
            scope.launch {
                _stateFlow.emit(BackgroundManagerState.AllActivitiesDestroyed)
            }
        }
    }

    override fun onActivityPaused(p0: Activity) {}

    override fun onActivityResumed(p0: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

}

enum class BackgroundManagerState {
    EnterForeground, EnterBackground, AllActivitiesDestroyed
}