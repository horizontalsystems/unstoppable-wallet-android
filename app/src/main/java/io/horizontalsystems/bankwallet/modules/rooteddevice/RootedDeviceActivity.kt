package io.horizontalsystems.bankwallet.modules.rooteddevice

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.databinding.ActivityRootedDeviceBinding
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow

class RootedDeviceActivity : AppCompatActivity() {

    private val viewModel by viewModels<RootedDeviceViewModel> { RootedDeviceModule.Factory() }
    private lateinit var binding: ActivityRootedDeviceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRootedDeviceBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewModel.openMainActivity.observe(this, {
            finish()
        })

        binding.buttonUnderstandCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 50.dp),
                    title = getString(R.string.RootedDevice_Button_Understand),
                    onClick = {
                        viewModel.ignoreRootedDeviceWarningButtonClicked()
                    }
                )
            }
        }
    }

    override fun onBackPressed() {
        //disable close with back button
    }
}
