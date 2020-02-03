package io.horizontalsystems.bankwallet.ui.helpers

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object HudHelper {

    private var toast: Toast? = null

    enum class ToastDuration(val milliseconds: Long) {
        SHORT(800), LONG(2000)
    }

    fun showSuccessMessage(text: Int, duration: ToastDuration = ToastDuration.SHORT) {
        showHudNotification(App.instance.getString(text), R.color.green_d, duration.milliseconds)
    }

    fun showErrorMessage(textRes: Int) {
        showErrorMessage(App.instance.getString(textRes))
    }

    fun showErrorMessage(text: String, duration: ToastDuration = ToastDuration.LONG) {
        showHudNotification(text, R.color.red_d, duration.milliseconds)
    }

    private fun showHudNotification(text: String, backgroundColor: Int, durationInMillis: Long) {
        toast?.cancel()

        val toast = Toast.makeText(App.instance, text, Toast.LENGTH_SHORT)

        val toastText = toast.view.findViewById(android.R.id.message) as TextView
        toastText.setTextColor(ContextCompat.getColor(toast.view.context, R.color.white))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            toast.view.background.colorFilter = BlendModeColorFilter(backgroundColor, BlendMode.SRC_ATOP)
        } else {
            toast.view.background.setColorFilter(backgroundColor, PorterDuff.Mode.SRC_ATOP)
        }
        toast.setGravity(Gravity.TOP, 0, 120)
        toast.show()

        Handler().postDelayed({ toast.cancel() }, durationInMillis)
    }
}
