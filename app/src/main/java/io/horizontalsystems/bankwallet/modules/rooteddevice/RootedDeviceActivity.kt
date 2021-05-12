package io.horizontalsystems.bankwallet.modules.rooteddevice

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.activity_rooted_device.*

class RootedDeviceActivity : AppCompatActivity() {

    private val viewModel by viewModels<RootedDeviceViewModel> { RootedDeviceModule.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rooted_device)

        understandButton.setOnClickListener {
            viewModel.ignoreRootedDeviceWarningButtonClicked()
        }

        viewModel.openMainActivity.observe(this, {
            finish()
        })
    }

    override fun onBackPressed() {
        //disable close with back button
    }
}
