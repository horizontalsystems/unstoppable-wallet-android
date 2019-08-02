package io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.subviews.ConfirmationMemoView
import io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.subviews.ConfirmationPrimaryView
import io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.subviews.ConfirmationSecondaryDataView
import io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.subviews.ConfirmationSendButtonView
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_send_confirmation.*


class SendConfirmationActivity : BaseActivity() {

    private lateinit var mainViewModel: SendConfirmationViewModel
    private var memoViewItem: ConfirmationMemoView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_confirmation)

        val confirmationInfo = intent.getParcelableExtra<SendConfirmationInfo>(SendConfirmationModule.ConfirmationInfoKey)

        shadowlessToolbar.bind(
                title = getString(R.string.Send_DialogTitle),
                rightBtnItem = TopMenuItem(R.drawable.close) { onBackPressed() }
        )

        mainViewModel = ViewModelProviders.of(this).get(SendConfirmationViewModel::class.java)
        mainViewModel.init(confirmationInfo)

        mainViewModel.addPrimaryDataViewItem.observe(this, Observer { primaryViewItem ->
            val primaryItemView = ConfirmationPrimaryView(this)
            primaryItemView.bind(primaryViewItem, { mainViewModel.delegate.onReceiverClick() })
            confirmationLinearLayout.addView(primaryItemView)
        })

        mainViewModel.addMemoViewItem.observe(this, Observer {
            memoViewItem = ConfirmationMemoView(this)
            confirmationLinearLayout.addView(memoViewItem)
        })

        mainViewModel.showCopied.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied, 500)
        })

        mainViewModel.addSecondaryDataViewItem.observe(this, Observer { secondaryData ->
            val secondaryDataView = ConfirmationSecondaryDataView(this)
            secondaryDataView.bind(secondaryData)
            confirmationLinearLayout.addView(secondaryDataView)
        })

        mainViewModel.addSendButton.observe(this, Observer {
            val sendButtonView = ConfirmationSendButtonView(this)
            sendButtonView.bind(R.string.Send_Button_Send)

            sendButtonView.setOnClickListener {
                mainViewModel.delegate.onSendClick()
                sendButtonView.bind(R.string.Send_Sending, false, true)
            }

            confirmationLinearLayout.addView(sendButtonView)
        })

        mainViewModel.memoForSend.observe(this, Observer {
            mainViewModel.delegate.send(memoViewItem?.getMemo())
        })

        mainViewModel.sendWithMemo.observe(this, Observer { memo ->
            val returnIntent = Intent()
            memo?.let{ returnIntent.putExtra(SendModule.MEMO_KEY, memo) }
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        })

    }

}
