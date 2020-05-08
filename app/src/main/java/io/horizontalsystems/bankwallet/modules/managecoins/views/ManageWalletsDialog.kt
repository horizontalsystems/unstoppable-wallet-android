package io.horizontalsystems.bankwallet.modules.managecoins.views

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.fragment_bottom_manage_wallets.*

class ManageWalletsDialog(
        private val listener: Listener,
        private val coin: Coin,
        private val predefinedAccountType: PredefinedAccountType)
    : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickCreateKey(predefinedAccountType: PredefinedAccountType)
        fun onClickRestoreKey(predefinedAccountType: PredefinedAccountType) {}
        fun onCancel() {}
    }

    private lateinit var addKeyInfo: TextView
    private lateinit var btnCreateKey: Button
    private lateinit var btnRestoreKey: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_manage_wallets)

        setTitle(activity?.getString(R.string.AddCoin_Title, coin.code))
        setSubtitle(getString(R.string.AddCoin_Subtitle, getString(predefinedAccountType.title)))
        context?.let { setHeaderIcon(LayoutHelper.getCoinDrawableResource(it, coin.code)) }

        addKeyInfo = view.findViewById(R.id.addKeyInfo)
        btnCreateKey = view.findViewById(R.id.primaryActionBtn)
        btnRestoreKey = view.findViewById(R.id.secondaryActionBtn)

        btnCreateKey.isVisible = predefinedAccountType.isCreationSupported()

        val walletName = getString(predefinedAccountType.title)
        val descriptionText = if (predefinedAccountType.isCreationSupported()) R.string.AddCoin_Description else R.string.AddCoin_CreationNotSupportedDescription
        addKeyInfo.text = getString(
                descriptionText,
                walletName,
                coin.title,
                walletName,
                getString(predefinedAccountType.coinCodes)
        )

        bindActions()
    }

    override fun close() {
        super.close()
        listener.onCancel()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener.onCancel()
    }

    private fun bindActions() {
        btnCreateKey.setOnClickListener {
            listener.onClickCreateKey(predefinedAccountType)
            dismiss()
        }

        btnRestoreKey.setOnClickListener {
            listener.onClickRestoreKey(predefinedAccountType)
            dismiss()
        }
    }

    companion object {
        fun show(activity: FragmentActivity, listener: Listener, coin: Coin, predefinedAccountType: PredefinedAccountType) {
            val fragment = ManageWalletsDialog(listener, coin, predefinedAccountType)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_manage_wallets_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
