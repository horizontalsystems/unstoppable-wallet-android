package io.horizontalsystems.core.helpers

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.horizontalsystems.core.R

object HudHelper {

    private var toast: Toast? = null

    enum class ToastDuration(val duration: Int) {
        SHORT(Toast.LENGTH_SHORT), LONG(Toast.LENGTH_LONG)
    }

    fun showSuccessMessage(context: Context, resId: Int, duration: ToastDuration = ToastDuration.SHORT) {
        showHudNotification(context, context.getString(resId), R.color.green_d, duration)
    }

    fun showErrorMessage(context: Context, textRes: Int) {
        showErrorMessage(context, context.getString(textRes))
    }

    fun showErrorMessage(context: Context, text: String) {
        showHudNotification(context, text, R.color.red_d, ToastDuration.LONG)
    }

    private fun showHudNotification(context: Context, text: String, backgroundColor: Int, toastDuration: ToastDuration) {
        this.toast?.cancel()

        val toast = Toast.makeText(context, text, toastDuration.duration)
        (toast.view as? LinearLayout)?.gravity = Gravity.CENTER // to align text in center for Xiaomi Mi Mix 2
        val toastText = toast.view.findViewById(android.R.id.message) as TextView
        toastText.setTextColor(getColor(toast.view.context, R.color.white))
        toast.view.background.setTint(getColor(toast.view.context, backgroundColor))
        toast.setGravity(Gravity.TOP, 0, 120)
        toast.show()

        this.toast = toast
    }

    private fun getColor(context: Context, colorId: Int) = ContextCompat.getColor(context, colorId)

}
