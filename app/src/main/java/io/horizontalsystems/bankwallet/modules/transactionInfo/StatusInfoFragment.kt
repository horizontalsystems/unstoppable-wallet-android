package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseDialogFragment
import io.horizontalsystems.bankwallet.databinding.FragmentStatusInfoBinding
import io.horizontalsystems.core.dismissOnBackPressed

class StatusInfoFragment : BaseDialogFragment() {

    private var _binding: FragmentStatusInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusInfoBinding.inflate(inflater, container, false)
        val view = binding.root
        dialog?.window?.setWindowAnimations(R.style.BottomDialogLargeAnimation)
        dialog?.dismissOnBackPressed { dismiss() }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.inflateMenu(R.menu.close_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menuClose -> {
                    dismiss()
                    true
                }
                else -> super.onOptionsItemSelected(menuItem)
            }
        }
    }
}
