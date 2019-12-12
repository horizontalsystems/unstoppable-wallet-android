package io.horizontalsystems.bankwallet.viewHelpers

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R

//  LayoutInflater

fun inflate(parent: ViewGroup, layout: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(parent.context).inflate(layout, parent, attachToRoot)
}

//  Dialog

fun bottomDialog(activity: FragmentActivity?, view: View): Dialog {
    val builder = AlertDialog.Builder(activity, R.style.BottomDialog).apply { setView(view) }
    val dialog = builder.create()

    dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    dialog.window?.setGravity(Gravity.BOTTOM)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    return dialog as Dialog
}

//  Extensions

fun View.showIf(condition: Boolean, hideType: Int = View.GONE) {
    visibility = if (condition) View.VISIBLE else hideType
}
