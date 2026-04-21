package io.horizontalsystems.core

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

class BackgroundManager(application: Application) : Application.ActivityLifecycleCallbacks {

    private val scope = CoroutineScope(Executors.newFixedThreadPool(2).asCoroutineDispatcher())
    private val _stateFlow: MutableStateFlow<BackgroundManagerState> = MutableStateFlow(BackgroundManagerState.Unknown)
    val stateFlow: StateFlow<BackgroundManagerState>
        get() = _stateFlow

    var onBeforeEnterBackground: (() -> Unit)? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    @Volatile
    private var lastResumedActivity: WeakReference<AppCompatActivity>? = null

    val currentActivity: AppCompatActivity?
        get() = lastResumedActivity?.get()?.takeIf { !it.isDestroyed }

    private var foregroundActivityCount: Int = 0

    val inForeground: Boolean
        @Synchronized get() = foregroundActivityCount > 0
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
                onBeforeEnterBackground?.invoke()
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

        if (lastResumedActivity?.get() === activity) {
            lastResumedActivity = null
        }
    }

    override fun onActivityPaused(p0: Activity) = Unit

    override fun onActivityResumed(activity: Activity) {
        lastResumedActivity = (activity as? AppCompatActivity)?.let { WeakReference(it) }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

}

enum class BackgroundManagerState {
    Unknown,
    EnterForeground,
    EnterBackground,
    AllActivitiesDestroyed
}