package io.horizontalsystems.bankwallet.modules.settings.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody
import io.horizontalsystems.core.findNavController

class PrivacyFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                PrivacyScreen(
                    findNavController(),
                )
            }
        }
    }

}

@Composable
private fun PrivacyScreen(navController: NavController) {
    ComposeAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = TranslatableString.ResString(R.string.Settings_Privacy),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                InfoTextBody(
                    text = stringResource(R.string.Privacy_Information),
                )

                BulletedText(R.string.Privacy_BulletedText1)
                BulletedText(R.string.Privacy_BulletedText2)
                BulletedText(R.string.Privacy_BulletedText3)
                BulletedText(R.string.Privacy_BulletedText4)
                Spacer(modifier = Modifier.height(32.dp))
            }

            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.FooterText),
                style = ComposeAppTheme.typography.caption,
                color = ComposeAppTheme.colors.grey,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun BulletedText(@StringRes text: Int) {
    Row(Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "\u2022 ",
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.bran,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(text),
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.bran,
            modifier = Modifier.padding(end = 32.dp)
        )
    }

}