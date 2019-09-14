package io.horizontalsystems.bankwallet.modules.pin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.pin.main.PinContainerModule
import io.horizontalsystems.bankwallet.modules.pin.main.PinContainerPresenter
import io.horizontalsystems.bankwallet.modules.pin.main.PinContainerRouter
import kotlinx.android.synthetic.main.activity_pin_container.*


class PinActivity : BaseActivity() {

    private lateinit var presenter: PinContainerPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_container)

        val pinFragment = PinFragment().apply {
            arguments = intent.extras
        }

        val showCancelButton = intent?.extras?.getBoolean(keyShowCancel) ?: true
        val showRates = intent?.extras?.getBoolean(keyShowRates) ?: false

        val presenter = ViewModelProviders.of(this, PinContainerModule.Factory(showCancelButton)).get(PinContainerPresenter::class.java)
        val router = presenter.router as PinContainerRouter

        subscribeToRouterEvents(router)

        if (showRates) {
            viewPager2.visibility = View.VISIBLE
            circleIndicator.visibility = View.VISIBLE
            fragmentContainer.visibility = View.GONE

            viewPager2.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = 2
                override fun createFragment(position: Int): Fragment = when (position) {
                    0 -> pinFragment
                    else -> RatesFragment()
                }
            }

            circleIndicator.setViewPager(viewPager2)
        } else {
            viewPager2.visibility = View.GONE
            circleIndicator.visibility = View.GONE
            fragmentContainer.visibility = View.VISIBLE

            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                        .add(R.id.fragmentContainer, pinFragment)
                        .commit()
            }
        }

    }

    private fun subscribeToRouterEvents(router: PinContainerRouter) {
        router.closeApplication.observe(this, Observer {
            finishAffinity()
        })

        router.closeActivity.observe(this, Observer {
            setResult(PinModule.RESULT_CANCELLED)
            finish()
        })
    }

    override fun onBackPressed() {
        presenter.onBackPressed()
    }

    companion object {

        const val keyInteractionType = "interaction_type"
        const val keyShowCancel = "show_cancel"
        const val keyShowRates = "show_rates"

        fun startForResult(context: AppCompatActivity, interactionType: PinInteractionType, requestCode: Int = 0, showCancel: Boolean = true, showRates: Boolean = false) {
            val intent = Intent(context, PinActivity::class.java)
            intent.putExtra(keyInteractionType, interactionType)
            intent.putExtra(keyShowCancel, showCancel)
            intent.putExtra(keyShowRates, showRates)

            context.startActivityForResult(intent, requestCode)
        }
    }
}
