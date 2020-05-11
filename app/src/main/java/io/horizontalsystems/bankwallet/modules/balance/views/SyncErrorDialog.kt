package io.horizontalsystems.bankwallet.modules.balance.views

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_bottom_sync_error.*

class SyncErrorDialog(private val listener: Listener, private val coin: Coin) : BaseBottomSheetDialogFragment() {

    interface Listener {
        fun onClickRetry()
        fun onClickChangeSource()
        fun onClickReport()
        fun onCancel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.fragment_bottom_sync_error)

        setTitle(activity?.getString(R.string.BalanceSyncError_Title))
        setSubtitle(coin.title)
        setHeaderIcon(R.drawable.ic_attention_red)

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
        retryBtn.setOnClickListener {
            listener.onClickRetry()
            dismiss()
        }

        changeSourceBtn.setOnClickListener {
            listener.onClickChangeSource()
            dismiss()
        }

        reportBtn.setOnClickListener {
            listener.onClickReport()
            dismiss()
        }
    }

    companion object {
        fun show(activity: FragmentActivity, coin: Coin, listener: Listener) {
            val fragment = SyncErrorDialog(listener, coin)
            val transaction = activity.supportFragmentManager.beginTransaction()

            transaction.add(fragment, "bottom_sync_error")
            transaction.commitAllowingStateLoss()
        }
    }
}
