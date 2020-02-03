package io.horizontalsystems.pin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.core.CoreActivity
import io.horizontalsystems.core.putParcelableExtra

class PinActivity : CoreActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val pinFragment = PinFragment().apply {
            arguments = intent.extras
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragmentContainer, pinFragment)
                    .commit()
        }
    }

    override fun onBackPressed() {
        setResult(PinModule.RESULT_CANCELLED)
        finish()
    }

    companion object {

        fun startForResult(context: AppCompatActivity, interactionType: PinInteractionType, requestCode: Int = 0, showCancel: Boolean = true) {
            val intent = Intent(context, PinActivity::class.java)
            intent.putParcelableExtra(PinModule.keyInteractionType, interactionType)
            intent.putExtra(PinModule.keyShowCancel, showCancel)

            context.startActivityForResult(intent, requestCode)
        }
    }
}
