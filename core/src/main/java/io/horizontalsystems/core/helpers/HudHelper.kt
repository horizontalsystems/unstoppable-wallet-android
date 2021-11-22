package io.horizontalsystems.core.helpers

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import io.horizontalsystems.core.R
import io.horizontalsystems.snackbar.CustomSnackbar
import io.horizontalsystems.snackbar.SnackbarDuration
import io.horizontalsystems.snackbar.SnackbarGravity

object HudHelper {

    fun showInProcessMessage(contenView: View, resId: Int, duration: SnackbarDuration = SnackbarDuration.SHORT, gravity: SnackbarGravity = SnackbarGravity.BOTTOM, showProgressBar: Boolean = true): CustomSnackbar? {
        return showHudNotification(contenView, contenView.context.getString(resId), R.color.grey, duration, gravity, showProgressBar = showProgressBar)
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

    fun showWarningMessage(
        contentView: View,
        resId: Int,
        duration: SnackbarDuration = SnackbarDuration.SHORT,
        gravity: SnackbarGravity = SnackbarGravity.BOTTOM
    ): CustomSnackbar? {
        return showHudNotification(
            contentView,
            contentView.context.getString(resId),
            R.color.grey,
            duration,
            gravity,
            showWarningIcon = true
        )
    }

    fun vibrate(context: Context) {
        val vibratorService = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        val vibrationEffect = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)

        vibratorService?.vibrate(vibrationEffect)
    }

    private fun showHudNotification(
        contentView: View,
        text: String,
        backgroundColor: Int,
        duration: SnackbarDuration,
        gravity: SnackbarGravity,
        showWarningIcon: Boolean = false,
        showProgressBar: Boolean = false
    ): CustomSnackbar? {

        val snackbar = CustomSnackbar.make(contentView, text, backgroundColor, duration, gravity, showProgressBar, showWarningIcon)
        snackbar?.show()

        return snackbar
    }
}
