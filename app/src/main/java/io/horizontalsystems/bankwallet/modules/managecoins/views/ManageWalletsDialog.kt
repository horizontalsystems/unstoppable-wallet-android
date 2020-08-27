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
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper

class ManageWalletsDialog : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickCreateKey(predefinedAccountType: PredefinedAccountType)
        fun onClickRestoreKey(predefinedAccountType: PredefinedAccountType) {}
        fun onCancel() {}
    }

    private lateinit var addKeyInfo: TextView
    private lateinit var btnCreateKey: Button
    private lateinit var btnRestoreKey: Button

    private var listener: Listener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_manage_wallets)

        val coin = requireArguments().getParcelable<Coin>("coin")!!
        val predefinedAccountType = requireArguments().getParcelable<PredefinedAccountType>("predefinedAccountType")!!

        setTitle(activity?.getString(R.string.AddCoin_Title, coin.code))
        setSubtitle(getString(R.string.AddCoin_Subtitle, getString(predefinedAccountType.title)))
        context?.let { setHeaderIconDrawable(AppLayoutHelper.getCoinDrawable(it, coin.code, coin.type)) }

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

        bindActions(predefinedAccountType)
    }

    override fun close() {
        super.close()
        listener?.onCancel()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener?.onCancel()
    }

    private fun bindActions(predefinedAccountType: PredefinedAccountType) {
        btnCreateKey.setOnClickListener {
            listener?.onClickCreateKey(predefinedAccountType)
            dismiss()
        }

        btnRestoreKey.setOnClickListener {
            listener?.onClickRestoreKey(predefinedAccountType)
            dismiss()
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    companion object {
        @JvmStatic
        fun show(activity: FragmentActivity, coin: Coin, predefinedAccountType: PredefinedAccountType) {
            val fragment = ManageWalletsDialog().apply {
                arguments = Bundle(2).apply {
                    putParcelable("coin", coin)
                    putParcelable("predefinedAccountType", predefinedAccountType)
                }
            }

            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_manage_wallets_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
