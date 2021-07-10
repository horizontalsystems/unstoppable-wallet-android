package io.horizontalsystems.bankwallet.modules.managewallets.view

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment

class AddTokenDialog(private val listener: Listener)
    : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickAddErc20Token()
        fun onClickAddBep20Token()
        fun onClickAddBep2Token()
    }

    private lateinit var erc20TokensBtn: Button
    private lateinit var bep20TokensBtn: Button
    private lateinit var bep2TokensBtn: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_add_token)

        erc20TokensBtn = view.findViewById(R.id.erc20TokensBtn)
        bep20TokensBtn = view.findViewById(R.id.bep20TokensBtn)
        bep2TokensBtn = view.findViewById(R.id.bep2TokensBtn)

        setTitle(activity?.getString(R.string.AddTokenDialog_Title))
        setSubtitle(getString(R.string.AddTokenDialog_Subtitle))
        setHeaderIcon(R.drawable.ic_plus_circled)

        bindActions()
    }

    private fun bindActions() {
        erc20TokensBtn.setOnClickListener {
            listener.onClickAddErc20Token()
            dismiss()
        }
        bep20TokensBtn.setOnClickListener {
            listener.onClickAddBep20Token()
            dismiss()
        }
        bep2TokensBtn.setOnClickListener {
            listener.onClickAddBep2Token()
            dismiss()
        }
    }

    companion object {
        fun show(activity: FragmentActivity, listener: Listener) {
            val fragment = AddTokenDialog(listener)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_add_token_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
