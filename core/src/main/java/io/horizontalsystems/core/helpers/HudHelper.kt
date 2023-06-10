package io.horizontalsystems.core.helpers

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.annotation.DrawableRes
import io.horizontalsystems.core.CustomSnackbar
import io.horizontalsystems.core.R
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.SnackbarGravity

object HudHelper {

    fun showInProcessMessage(
        contenView: View,
        resId: Int,
        duration: SnackbarDuration = SnackbarDuration.SHORT,
        gravity: SnackbarGravity = SnackbarGravity.BOTTOM,
        showProgressBar: Boolean = true
    ): CustomSnackbar? {
        return showHudNotification(
            contentView = contenView,
            text = contenView.context.getString(resId),
            backgroundColor = R.color.grey,
            duration = duration,
            gravity = gravity,
            showProgressBar = showProgressBar
        )
    }

    fun showSuccessMessage(
        contenView: View,
        resId: Int,
        duration: SnackbarDuration = SnackbarDuration.SHORT,
        gravity: SnackbarGravity = SnackbarGravity.BOTTOM,
        @DrawableRes icon: Int? = null,
        iconTint: Int? = null,
    ): CustomSnackbar? {
        return showHudNotification(
            contentView = contenView,
            text = contenView.context.getString(resId),
            backgroundColor = R.color.green_d,
            duration = duration,
            gravity = gravity,
            icon = icon,
            iconTint = iconTint
        )
    }

    fun showSuccessMessage(
        contenView: View,
        text: String,
        duration: SnackbarDuration = SnackbarDuration.SHORT,
        gravity: SnackbarGravity = SnackbarGravity.BOTTOM
    ): CustomSnackbar? {
        return showHudNotification(
            contentView = contenView,
            text = text,
            backgroundColor = R.color.green_d,
            duration = duration,
            gravity = gravity,
        )
    }

    fun showErrorMessage(
        contenView: View,
        textRes: Int,
        gravity: SnackbarGravity = SnackbarGravity.BOTTOM
    ) {
        showErrorMessage(contenView, contenView.context.getString(textRes), gravity)
    }

    fun showErrorMessage(
        contenView: View,
        text: String,
        gravity: SnackbarGravity = SnackbarGravity.BOTTOM
    ): CustomSnackbar? {
        return showHudNotification(
            contentView = contenView,
            text = text,
            backgroundColor = R.color.red_d,
            duration = SnackbarDuration.LONG,
            gravity = gravity,
        )
    }

    fun showErrorMessage(
        contenView: View,
        resId: Int,
        duration: SnackbarDuration = SnackbarDuration.SHORT,
        gravity: SnackbarGravity = SnackbarGravity.BOTTOM,
        @DrawableRes icon: Int? = null,
        iconTint: Int? = null,
    ): CustomSnackbar? {
        return showHudNotification(
            contentView = contenView,
            text = contenView.context.getString(resId),
            backgroundColor = R.color.red_d,
            duration = duration,
            gravity = gravity,
            icon = icon,
            iconTint = iconTint
        )
    }

    fun showWarningMessage(
        contentView: View,
        resId: Int,
        duration: SnackbarDuration = SnackbarDuration.SHORT,
        gravity: SnackbarGravity = SnackbarGravity.BOTTOM
    ): CustomSnackbar? {
        return showHudNotification(
            contentView = contentView,
            text = contentView.context.getString(resId),
            backgroundColor = R.color.grey,
            duration = duration,
            gravity = gravity,
            icon = R.drawable.ic_attention_24,
            iconTint = R.color.jacob
        )
    }

    fun vibrate(context: Context) {
        val vibratorService = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        val vibrationEffect = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)

        vibratorService?.vibrate(vibrationEffect)
    }

    private fun showHudNotification(
        contentView: View,
        text: String,
        backgroundColor: Int,
        duration: SnackbarDuration,
        gravity: SnackbarGravity,
        showProgressBar: Boolean = false,
        @DrawableRes icon: Int? = null,
        iconTint: Int? = null,
    ): CustomSnackbar? {

        val snackbar = CustomSnackbar.make(
            contentView,
            text,
            backgroundColor,
            duration,
            gravity,
            showProgressBar,
            icon,
            iconTint
        )
        snackbar?.show()

        return snackbar
    }
}
