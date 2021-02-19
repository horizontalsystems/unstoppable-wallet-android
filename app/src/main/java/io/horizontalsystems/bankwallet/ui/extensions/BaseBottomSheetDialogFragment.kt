package io.horizontalsystems.bankwallet.ui.extensions

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.horizontalsystems.bankwallet.R

open class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var content: ViewStub? = null
    private var txtTitle: TextView? = null
    private var txtSubtitle: TextView? = null
    private var headerIcon: ImageView? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private val disableCloseOnSwipeBehavior = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    var shouldCloseOnSwipe: Boolean = true
        set(shouldClose) {
            field = shouldClose
            setUpSwipeBehavior()
        }

    override fun getTheme(): Int {
        return R.style.BottomDialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.constraint_layout_with_header, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        content = view.findViewById(R.id.content)
        txtTitle = view.findViewById(R.id.txtTitle)
        txtSubtitle = view.findViewById(R.id.txtSubtitle)
        headerIcon = view.findViewById(R.id.headerIcon)

        view.findViewById<ImageView>(R.id.closeButton)?.setOnClickListener { close() }

        dialog?.setOnShowListener {
            onShow()
            // To avoid the bottom sheet stuck in between
            dialog?.findViewById<View>(R.id.design_bottom_sheet)?.let {
                bottomSheetBehavior = BottomSheetBehavior.from(it).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    isHideable = true
                    skipCollapsed = true
                }
                setUpSwipeBehavior()
            }
        }
    }

    private fun setUpSwipeBehavior() {
        if (shouldCloseOnSwipe)
            bottomSheetBehavior?.removeBottomSheetCallback(disableCloseOnSwipeBehavior)
        else
            bottomSheetBehavior?.addBottomSheetCallback(disableCloseOnSwipeBehavior)
    }

    open fun close() {
        dismiss()
    }

    open fun onShow() {

    }

    fun setContentView(@LayoutRes resource: Int) {
        content?.layoutResource = resource
        content?.inflate()
    }

    fun setTitle(title: String?) {
        txtTitle?.text = title
    }

    fun setSubtitle(subtitle: String?) {
        txtSubtitle?.text = subtitle
    }

    fun setHeaderIcon(@DrawableRes resource: Int) {
        headerIcon?.setImageResource(resource)
    }

    fun setHeaderIconTint(@ColorRes resource: Int) {
        headerIcon?.imageTintList = ColorStateList.valueOf(requireContext().getColor(resource))
    }

    fun setHeaderIconDrawable(drawable: Drawable?) {
        headerIcon?.setImageDrawable(drawable)
    }

}
