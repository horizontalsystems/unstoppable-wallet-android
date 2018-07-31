package bitcoin.wallet.viewHelpers

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import bitcoin.wallet.R
import bitcoin.wallet.core.App



object HudHelper {

    private var toast: Toast? = null

    fun showCopySuccess() {
        showHudNotification(R.string.receive_bottom_sheet_copied, R.drawable.ic_done_checkmark_green)
    }

    fun cancelToast() {
        toast?.cancel()
    }

    private fun showHudNotification(text: Int, icon: Int) {
        cancelToast()

        val inflater = App.instance.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.custom_toast_view, null)
        view.findViewById<ImageView>(R.id.toastIcon).setImageResource(icon)
        view.findViewById<TextView>(R.id.infoText).setText(text)
        toast = Toast(App.instance)
        toast?.view = view
        toast?.setGravity(Gravity.TOP, 0, 50)
        toast?.duration = Toast.LENGTH_SHORT
        toast?.show()
    }
}
