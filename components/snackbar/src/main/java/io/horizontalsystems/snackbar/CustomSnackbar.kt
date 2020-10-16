package io.horizontalsystems.snackbar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar


enum class SnackbarDuration(val value: Int) {
    SHORT(Snackbar.LENGTH_SHORT),
    LONG(Snackbar.LENGTH_LONG),
    INDEFINITE(Snackbar.LENGTH_INDEFINITE),
}

// Snackbar placement on screen
enum class SnackbarGravity(val value: Int) {
    TOP(1),
    BOTTOM(2),
    TOP_OF_VIEW(3),
    BOTTOM_OF_VIEW(4)
}

class CustomSnackbar (
        parent: ViewGroup,
        content: View,
        contentViewCallback: ContentViewCallback)
    : BaseTransientBottomBar<CustomSnackbar?>(parent, content, contentViewCallback) {

    init {
        getView().setBackgroundColor(ContextCompat.getColor(view.context, android.R.color.transparent))
        getView().setPadding(0,0,0,0)
    }

    class ContentViewCallback(private val view: View) : com.google.android.material.snackbar.ContentViewCallback {
        override fun animateContentIn(delay: Int, duration: Int) {}

        override fun animateContentOut(delay: Int, duration: Int) {}
    }

    companion object {
        fun make(viewGroup: ViewGroup,
                 text: String,
                 backgroundColor: Int,
                 duration: SnackbarDuration,
                 gravity: SnackbarGravity,
                 showProgressBar: Boolean): CustomSnackbar {

            val parent = viewGroup.findSuitableParent() ?: throw IllegalArgumentException(
                    "No suitable parent found from the given view. Please provide a valid view."
            )
            val inflater = LayoutInflater.from(viewGroup.context)
            val view = inflater.inflate(R.layout.view_custom_snackbar, parent, false)

            val callback = ContentViewCallback(view)
            val customSnackbar = CustomSnackbar(parent, view, callback)

            val snackbarText = view.findViewById<TextView>(R.id.snackbarText)
            val contentLayout = view.findViewById<LinearLayout>(R.id.snackbarContentLayout)

            snackbarText.text = text
            customSnackbar.duration = duration.value
            customSnackbar.animationMode = ANIMATION_MODE_FADE

            if(showProgressBar){
                val progressbar = view.findViewById<ProgressBar>(R.id.snackbarProgressbar)
                progressbar.visibility = View.VISIBLE
            }

            contentLayout.background.setTint(ContextCompat.getColor(viewGroup.context, backgroundColor))

            if(gravity == SnackbarGravity.TOP_OF_VIEW)
                customSnackbar.anchorView = viewGroup


            return customSnackbar
        }
    }
}

internal fun View?.findSuitableParent(): ViewGroup? {
    var view = this
    var fallback: ViewGroup? = null
    do {
        if (view is CoordinatorLayout) {
            return view
        } else if (view is FrameLayout) {
            if (view.id == android.R.id.content) {
                return view
            } else {
                fallback = view
            }
        }

        if (view != null) {
            val parent = view.parent
            view = if (parent is View) parent else null
        }
    } while (view != null)

    return fallback
}

