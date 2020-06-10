package io.horizontalsystems.core.helpers

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import io.horizontalsystems.core.R

object HudHelper {

    enum class SnackbarDuration(val duration: Int) {
        SHORT(Snackbar.LENGTH_SHORT), LONG(Snackbar.LENGTH_LONG)
    }

    // Snackbar placement on screen
    enum class SnackbarGravity {
        TOP,
        BOTTOM,
        TOP_OF_VIEW,
        BOTTOM_OF_VIEW
    }

    fun showSuccessMessage(contenView: View, resId: Int, duration: SnackbarDuration = SnackbarDuration.SHORT,
                           gravity: SnackbarGravity = SnackbarGravity.BOTTOM) : Snackbar {
        return showHudNotification(contenView, contenView.context.getString(resId), R.color.green_d, duration, gravity)
    }

    fun showErrorMessage(contenView: View, textRes: Int, gravity: SnackbarGravity = SnackbarGravity.BOTTOM) {
         showErrorMessage(contenView, contenView.context.getString(textRes), gravity)
    }

    fun showErrorMessage(contenView: View, text: String, gravity: SnackbarGravity = SnackbarGravity.BOTTOM) : Snackbar {
        return showHudNotification(contenView, text, R.color.red_d, SnackbarDuration.LONG, gravity)
    }

    private fun showHudNotification(contenView: View, text: String, backgroundColor: Int, duration: SnackbarDuration, gravity: SnackbarGravity)
            : Snackbar {

        return Snackbar.make(contenView, text, duration.duration).apply {

            if(gravity == SnackbarGravity.TOP_OF_VIEW)
                this.anchorView = contenView

            val textView = this.view.findViewById(R.id.snackbar_text) as TextView
            textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            textView.gravity  = Gravity.CENTER_HORIZONTAL

            this.view.background.setTint(getColor(this.view.context, backgroundColor))
            this.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
            this.show()
        }
    }

    private fun getColor(context: Context, colorId: Int) = ContextCompat.getColor(context, colorId)

}
