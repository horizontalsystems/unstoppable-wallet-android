package io.horizontalsystems.core.helpers

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.R

object HudHelper {

    private var toast: Toast? = null

    enum class ToastDuration(val duration: Int) {
        SHORT(Toast.LENGTH_SHORT), LONG(Toast.LENGTH_LONG)
    }

    fun showSuccessMessage(resId: Int, duration: ToastDuration = ToastDuration.SHORT) {
        showHudNotification(CoreApp.instance.getString(resId), R.color.green_d, duration)
    }

    fun showErrorMessage(textRes: Int) {
        showErrorMessage(CoreApp.instance.getString(textRes))
    }

    fun showErrorMessage(text: String) {
        showHudNotification(text, R.color.red_d, ToastDuration.LONG)
    }

    private fun showHudNotification(text: String, backgroundColor: Int, toastDuration: ToastDuration) {
        this.toast?.cancel()

        val toast = Toast.makeText(CoreApp.instance, text, toastDuration.duration)
        (toast.view as? LinearLayout)?.gravity = Gravity.CENTER // to align text in center for Xiaomi Mi Mix 2
        val toastText = toast.view.findViewById(android.R.id.message) as TextView
        toastText.setTextColor(getColor(toast.view.context, R.color.white))
        toast.view.background.setTint(getColor(toast.view.context, backgroundColor))
        toast.setGravity(Gravity.TOP, 0, 120)
        toast.show()

        this.toast = toast
    }

    private fun getColor(context: Context, colorId: Int) =
            ContextCompat.getColor(context, colorId)
}
