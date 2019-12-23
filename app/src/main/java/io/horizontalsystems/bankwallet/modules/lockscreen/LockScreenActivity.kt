package io.horizontalsystems.bankwallet.modules.lockscreen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.modules.pin.PinFragment
import io.horizontalsystems.bankwallet.modules.pin.PinInteractionType
import io.horizontalsystems.bankwallet.modules.pin.PinModule
import io.horizontalsystems.bankwallet.modules.ratelist.RatesFragment
import kotlinx.android.synthetic.main.activity_lockscreen.*


class LockScreenActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lockscreen)

        val pinFragment = PinFragment().apply {
            arguments = intent.extras
        }

        val fragments = listOf(pinFragment, RatesFragment())

        viewPager.adapter = LockScreenViewPagerAdapter(fragments, supportFragmentManager)

        circleIndicator.setViewPager(viewPager)
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    companion object {

        fun startForResult(context: AppCompatActivity, requestCode: Int = 0) {
            val intent = Intent(context, LockScreenActivity::class.java)
            intent.putParcelableExtra(PinModule.keyInteractionType, PinInteractionType.UNLOCK)
            intent.putExtra(PinModule.keyShowCancel, false)

            context.startActivityForResult(intent, requestCode)
        }
    }
}
