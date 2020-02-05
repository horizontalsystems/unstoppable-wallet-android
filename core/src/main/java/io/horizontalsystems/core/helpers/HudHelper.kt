package io.horizontalsystems.core.helpers

import android.content.Context
import android.os.Handler
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.R

object HudHelper {

    private var toast: Toast? = null

    enum class ToastDuration(val milliseconds: Long) {
        SHORT(800), LONG(2000)
    }

    fun showSuccessMessage(text: Int, duration: ToastDuration = ToastDuration.SHORT) {
        showHudNotification(CoreApp.instance.getString(text), R.color.green_d, duration.milliseconds)
    }

    fun showErrorMessage(textRes: Int) {
        showErrorMessage(CoreApp.instance.getString(textRes))
    }

    fun showErrorMessage(text: String, duration: ToastDuration = ToastDuration.LONG) {
        showHudNotification(text, R.color.red_d, duration.milliseconds)
    }

    private fun showHudNotification(text: String, backgroundColor: Int, durationInMillis: Long) {
        toast?.cancel()

        val toast = Toast.makeText(CoreApp.instance, text, Toast.LENGTH_SHORT)

        val toastText = toast.view.findViewById(android.R.id.message) as TextView
        toastText.setTextColor(getColor(toast.view.context, R.color.white))
        toast.view.background.setTint(getColor(toast.view.context, backgroundColor))
        toast.setGravity(Gravity.TOP, 0, 120)
        toast.show()

        Handler().postDelayed({ toast.cancel() }, durationInMillis)
    }

    private fun getColor(context: Context, colorId: Int) =
            ContextCompat.getColor(context, colorId)
}
