package io.horizontalsystems.bankwallet.viewHelpers

import android.graphics.PorterDuff
import android.support.v4.content.ContextCompat
import android.widget.TextView
import android.widget.Toast
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App


object HudHelper {

    private var toast: Toast? = null

    fun showSuccessMessage(text: Int) {
        showHudNotification(text, R.color.green_crypto)
    }

    fun showErrorMessage(text: Int) {
        showHudNotification(text, R.color.red_warning)
    }

    private fun showHudNotification(text: Int, backgroundColor: Int) {
        toast?.cancel()

        val toast = Toast.makeText(App.instance, text, Toast.LENGTH_SHORT)

        val toastText = toast.view.findViewById(android.R.id.message) as TextView
        toastText.setTextColor(ContextCompat.getColor(toast.view.context, R.color.white))
        toast.view.background.setColorFilter(ContextCompat.getColor(toast.view.context, backgroundColor), PorterDuff.Mode.SRC_IN)
        toast.show()
    }
}
