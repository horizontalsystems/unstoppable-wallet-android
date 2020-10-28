package io.horizontalsystems.core.navigation

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.customview.widget.Openable
import androidx.navigation.FloatingWindow
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.ui.AppBarConfiguration
import androidx.transition.TransitionManager
import io.horizontalsystems.core.R
import java.lang.ref.WeakReference
import java.util.regex.Matcher
import java.util.regex.Pattern


class NavDestinationChangeListener(
        toolbar: Toolbar,
        configuration: AppBarConfiguration,
        private val showBackIcon: Boolean) : NavController.OnDestinationChangedListener {

    @Nullable
    private var mOpenableLayoutWeakReference: WeakReference<Openable>? = null
    private var mToolbarWeakReference: WeakReference<Toolbar>? = WeakReference(toolbar)
    private var iconDrawable: Drawable? = null

    init {
        val mContext = toolbar.context
        iconDrawable = mContext?.let { ContextCompat.getDrawable(it, R.drawable.ic_back) }
        configuration.openableLayout?.let {
            mOpenableLayoutWeakReference = WeakReference(it)
        }
    }

    override fun onDestinationChanged(
            @NonNull controller: NavController,
            @NonNull destination: NavDestination,
            @Nullable arguments: Bundle?
    ) {

        if (destination is FloatingWindow) {
            return
        }

        val openableLayout: Openable? = mOpenableLayoutWeakReference?.get()
        if (mOpenableLayoutWeakReference != null && openableLayout == null) {
            controller.removeOnDestinationChangedListener(this)
            return
        }

        destination.label?.let { label ->
            // Fill in the data pattern with the args to build a valid URI
            val title = StringBuffer()
            val fillInPattern: Pattern = Pattern.compile("\\{(.+?)\\}")
            val matcher: Matcher = fillInPattern.matcher(label)
            while (matcher.find()) {
                val argName: String? = matcher.group(1)
                if (arguments != null && arguments.containsKey(argName)) {
                    matcher.appendReplacement(title, "")
                    title.append(arguments[argName].toString())
                } else {
                    throw IllegalArgumentException("Could not find " + argName + " in "
                            + arguments + " to fill label " + label)
                }
            }
            matcher.appendTail(title)
            setTitle(title)
        }

        setActionBarUpIndicator()
    }

    private fun setTitle(title: CharSequence?) {
        mToolbarWeakReference?.get()?.title = title
    }

    private fun setActionBarUpIndicator() {
        if (showBackIcon) {
            setNavigationIcon(iconDrawable)
        } else {
            setNavigationIcon(null)
        }
    }

    private fun setNavigationIcon(icon: Drawable?) {
        mToolbarWeakReference?.get()?.let { toolbar ->
            val useTransition = icon == null && toolbar.navigationIcon != null
            toolbar.navigationIcon = icon
            if (useTransition) {
                TransitionManager.beginDelayedTransition(toolbar)
            }
        }
    }
}
