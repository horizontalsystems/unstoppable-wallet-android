package io.horizontalsystems.bankwallet.modules.pin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.main.PinContainerViewModel

class PinActivity : BaseActivity() {

    private lateinit var viewModel: PinContainerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_container)

        val params = PinFragment().apply {
            arguments = intent.extras
        }

        val showCancelButton = intent?.extras?.getBoolean(keyShowCancel) ?: false

        viewModel = ViewModelProviders.of(this).get(PinContainerViewModel::class.java)
        viewModel.init(showCancelButton)

        viewModel.closeApplicationLiveEvent.observe(this, Observer {
            finishAffinity()
        })

        viewModel.closeActivityLiveEvent.observe(this, Observer {
            setResult(PinModule.RESULT_CANCELLED)
            finish()
        })

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, params)
                    .commit()
        }
    }

    override fun onBackPressed() {
        viewModel.delegate.onBackPressed()
    }

    companion object {

        const val keyInteractionType = "interaction_type"
        const val keyShowCancel = "show_cancel"

        fun startForResult(context: AppCompatActivity, interactionType: PinInteractionType, requestCode: Int = 0, showCancel: Boolean = true) {
            val intent = Intent(context, PinActivity::class.java)
            intent.putExtra(keyInteractionType, interactionType)
            intent.putExtra(keyShowCancel, showCancel)

            context.startActivityForResult(intent, requestCode)
        }
    }
}
