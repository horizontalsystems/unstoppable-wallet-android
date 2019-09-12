package io.horizontalsystems.bankwallet.modules.pin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R

class PinActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_container)

        if (savedInstanceState == null) {
            val params = PinFragment().apply {
                arguments = intent.extras
            }

            supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, params)
                    .commit()
        }
    }

    override fun onBackPressed() {
//        viewModel.delegate.onBackPressed()
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
