package io.horizontalsystems.bankwallet.core

import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.findNavController

abstract class BaseDialogFragment : DialogFragment() {

    override fun getTheme() = R.style.BottomDialogFullScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialog?.setOnShowListener {
            dialog?.findViewById<View>(R.id.design_bottom_sheet)?.let { view ->
                BottomSheetBehavior.from(view).apply {
                    peekHeight = Resources.getSystem().displayMetrics.heightPixels
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        findNavController().popBackStack()
    }
}
