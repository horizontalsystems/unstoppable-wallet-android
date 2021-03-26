package io.horizontalsystems.bankwallet.modules.noaccount

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.predefinedAccountType
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_bottom_manage_wallets.*

class NoAccountDialog : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickRestoreKey(predefinedAccountType: PredefinedAccountType, coin: Coin) {}
        fun onCancel() {}
    }

    private lateinit var viewModel: NoAccountViewModel
    private var listener: Listener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_manage_wallets)

        val coin = requireArguments().getParcelable<Coin>(COIN_KEY)!!

        viewModel = ViewModelProvider(this, NoAccountModule.Factory(coin))
                .get(NoAccountViewModel::class.java)

        val predefinedAccountType = coin.type.predefinedAccountType

        setTitle(activity?.getString(R.string.AddCoin_Title, coin.code))
        setSubtitle(getString(R.string.AddCoin_Subtitle, getString(predefinedAccountType?.title)))
        context?.let { setHeaderIconDrawable(AppLayoutHelper.getCoinDrawable(it, coin.type)) }

        val walletName = getString(predefinedAccountType.title)
        addKeyInfo.text = getString(
                R.string.AddCoin_Description,
                walletName,
                coin.title,
                walletName,
                getString(predefinedAccountType.coinCodes)
        )

        createBtn.setOnClickListener {
            viewModel.onClickCreateKey()
        }

        restoreBtn.setOnClickListener {
            listener?.onClickRestoreKey(coin.type.predefinedAccountType, coin)
            dismiss()
        }

        observe()
    }

    private fun observe() {
        viewModel.accountCreateErrorLiveEvent.observe(viewLifecycleOwner, Observer {
            listener?.onCancel()
            dismiss()
        })

        viewModel.accountCreateSuccessLiveEvent.observe(viewLifecycleOwner, Observer {
            HudHelper.showSuccessMessage(this.requireView(), R.string.Hud_Text_Done)
            Handler(Looper.getMainLooper()).postDelayed({
                dismiss()
            }, 1500)
        })
    }

    override fun close() {
        super.close()
        listener?.onCancel()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener?.onCancel()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }


    companion object {
        const val COIN_KEY = "coin_key"

        @JvmStatic
        fun show(supportFragmentManager: FragmentManager, coin: Coin) {
            val fragment = NoAccountDialog().apply {
                arguments = Bundle(1).apply {
                    putParcelable(COIN_KEY, coin)
                }
            }

            val transaction = supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_no_account_dialog")
            transaction.commitAllowingStateLoss()
        }
    }
}
