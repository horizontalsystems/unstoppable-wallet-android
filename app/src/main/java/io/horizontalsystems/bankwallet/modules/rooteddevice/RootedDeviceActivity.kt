package io.horizontalsystems.bankwallet.modules.rooteddevice

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import kotlinx.android.synthetic.main.activity_rooted_device.*

class RootedDeviceActivity : AppCompatActivity() {

    private val viewModel by viewModels<RootedDeviceViewModel> { RootedDeviceModule.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rooted_device)

        viewModel.openMainActivity.observe(this, {
            finish()
        })

        buttonUnderstandCompose.setContent {
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
