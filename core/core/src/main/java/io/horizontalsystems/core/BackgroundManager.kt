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
import java.util.WeakHashMap
import java.util.concurrent.Executors
import kotlin.reflect.KClass

class BackgroundManager(application: Application) : Application.ActivityLifecycleCallbacks {

    private val scope = CoroutineScope(Executors.newFixedThreadPool(2).asCoroutineDispatcher())
    private val _stateFlow: MutableStateFlow<BackgroundManagerState> = MutableStateFlow(BackgroundManagerState.Unknown)
    val stateFlow: StateFlow<BackgroundManagerState>
        get() = _stateFlow

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    private val activities = WeakHashMap<KClass<out Activity>, AppCompatActivity>()

    val currentActivity: AppCompatActivity?
        get() = activities.entries
            .firstOrNull { it.value?.isDestroyed == false }
            ?.value

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

        activities.remove(activity::class)
    }

    override fun onActivityPaused(p0: Activity) = Unit

    override fun onActivityResumed(activity: Activity) {
        activities[activity::class] = activity as? AppCompatActivity
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

}

enum class BackgroundManagerState {
    Unknown,
    EnterForeground,
    EnterBackground,
    AllActivitiesDestroyed
}