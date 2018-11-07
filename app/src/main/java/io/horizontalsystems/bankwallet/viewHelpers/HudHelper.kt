package io.horizontalsystems.bankwallet.viewHelpers

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App


object HudHelper {

    private var toast: Toast? = null

    fun showSuccessMessage(text: Int? = null, activity: Activity?) {
        showHudNotification(text, R.drawable.ic_done_checkmark_green, activity)
    }

    fun showNoInternetError(activity: Activity?) {
        showErrorMessage(R.string.hud_text_no_internet, activity)
    }

    fun showErrorMessage(text: Int, activity: Activity?) {
        showHudNotification(text, R.drawable.ic_error, activity, true)
    }

    fun showAlertMessage(text: Int, activity: Activity?) {
        showHudNotification(text, R.drawable.ic_alert, activity)
    }

    fun cancelToast() {
        toast?.cancel()
    }

    private fun showHudNotification(text: Int?, icon: Int, activity: Activity?, useIconTint: Boolean = false) {
        cancelToast()

        val inflater = activity?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layoutResource = if (text == null) R.layout.custom_toast_view_no_text else R.layout.custom_toast_view
        val view = inflater.inflate(layoutResource, null)
        view.findViewById<ImageView>(R.id.toastIcon).setImageResource(icon)
        if (useIconTint) {
            LayoutHelper.getAttr(R.attr.HudIconTint, activity.theme)?.let {
                view.findViewById<ImageView>(R.id.toastIcon).setColorFilter(it, android.graphics.PorterDuff.Mode.SRC_ATOP)
            }
        }

        text?.let {
            view.findViewById<TextView>(R.id.infoText)?.setText(it)
        }

        toast = Toast(App.instance)
        toast?.view = view
        toast?.setGravity(Gravity.CENTER, 0, -200)
        toast?.duration = Toast.LENGTH_SHORT
        toast?.show()
    }
}
