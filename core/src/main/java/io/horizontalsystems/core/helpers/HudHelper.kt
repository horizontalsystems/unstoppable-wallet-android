package io.horizontalsystems.core.helpers

import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.core.R
import io.horizontalsystems.snackbar.CustomSnackbar
import io.horizontalsystems.snackbar.SnackbarDuration
import io.horizontalsystems.snackbar.SnackbarGravity

object HudHelper {

    fun showInProcessMessage(contenView: View, resId: Int, duration: SnackbarDuration = SnackbarDuration.SHORT, gravity: SnackbarGravity = SnackbarGravity.BOTTOM): CustomSnackbar? {
        return showHudNotification(contenView, contenView.context.getString(resId), R.color.grey, duration, gravity, true)
    }

    fun showSuccessMessage(contenView: View, resId: Int, duration: SnackbarDuration = SnackbarDuration.SHORT, gravity: SnackbarGravity = SnackbarGravity.BOTTOM): CustomSnackbar? {
        return showHudNotification(contenView, contenView.context.getString(resId), R.color.green_d, duration, gravity, false)
    }

    fun showSuccessMessage(contenView: View, text: String, duration: SnackbarDuration = SnackbarDuration.SHORT, gravity: SnackbarGravity = SnackbarGravity.BOTTOM): CustomSnackbar? {
        return showHudNotification(contenView, text, R.color.green_d, duration, gravity, false)
    }

    fun showErrorMessage(contenView: View, textRes: Int, gravity: SnackbarGravity = SnackbarGravity.BOTTOM) {
        showErrorMessage(contenView, contenView.context.getString(textRes), gravity)
    }

    fun showErrorMessage(contenView: View, text: String, gravity: SnackbarGravity = SnackbarGravity.BOTTOM): CustomSnackbar? {
        return showHudNotification(contenView, text, R.color.red_d, SnackbarDuration.LONG, gravity, false)
    }

    private fun showHudNotification(
            contenView: View,
            text: String,
            backgroundColor: Int,
            duration: SnackbarDuration,
            gravity: SnackbarGravity,
            showProgressBar: Boolean = false
    ): CustomSnackbar? {

        val snackbar = CustomSnackbar.make(contenView as ViewGroup, text, backgroundColor, duration, gravity, showProgressBar)
        snackbar.show()

        return snackbar
    }
}
