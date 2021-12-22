package io.horizontalsystems.bankwallet.ui.extensions

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setImage
import io.horizontalsystems.bankwallet.databinding.ConstraintLayoutWithHeaderBinding
import io.horizontalsystems.bankwallet.modules.market.ImageSource

open class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private val disableCloseOnSwipeBehavior = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {}

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    var draggable: Boolean = true
        set(value) {
            field = value
            bottomSheetBehavior?.isHideable = value
        }

    var shouldCloseOnSwipe: Boolean = true
        set(shouldClose) {
            field = shouldClose
            setUpSwipeBehavior()
        }

    override fun getTheme(): Int {
        return R.style.BottomDialog
    }

    private var _binding: ConstraintLayoutWithHeaderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ConstraintLayoutWithHeaderBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        binding.content.layoutResource = resource
        binding.content.inflate()
    }

    fun setTitle(title: String?) {
        binding.txtTitle.text = title
    }

    fun setSubtitle(subtitle: String?) {
        binding.txtSubtitle.text = subtitle
    }

    fun setHeaderIcon(@DrawableRes resource: Int) {
        binding.headerIcon.setImageResource(resource)
    }

    fun setHeaderIconTint(@ColorRes resource: Int) {
        binding.headerIcon.imageTintList =
            ColorStateList.valueOf(requireContext().getColor(resource))
    }

    fun setHeaderIconDrawable(drawable: Drawable?) {
        binding.headerIcon.setImageDrawable(drawable)
    }

    fun setHeaderIcon(imageSource: ImageSource) {
        binding.headerIcon.setImage(imageSource)
    }

}
