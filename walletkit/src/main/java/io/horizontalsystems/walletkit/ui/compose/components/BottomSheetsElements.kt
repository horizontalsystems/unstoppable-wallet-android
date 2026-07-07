package io.horizontalsystems.walletkit.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme

@Composable
fun BottomSheetsElementsButtons(
    buttonPrimaryText: String,
    onClickPrimary: () -> Unit,
    buttonDefaultText: String? = null,
    onClickDefault: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        HsDivider()
        Spacer(modifier = Modifier.height(15.dp))
        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            title = buttonPrimaryText,
            onClick = onClickPrimary
        )
        buttonDefaultText?.let {
            Spacer(modifier = Modifier.height(16.dp))
            ButtonPrimaryDefault(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                title = buttonDefaultText,
                onClick = onClickDefault ?: {}
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BottomSheetsElementsText(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HsDivider()

        subhead2_grey(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
            text = text
        )
    }
}

@Composable
fun BottomSheetsElementsHeader(
    icon: Painter,
    title: String,
    subtitle: String,
    onClickClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 9.dp)
    ) {
        Image(
            modifier = Modifier.size(24.dp),
            painter = icon,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            headline2_leah(text = title)
            Spacer(modifier = Modifier.height(4.dp))
            subhead2_grey(text = subtitle)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onClickClose),
            painter = painterResource(R.drawable.icon_24_close_3),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey
        )
    }
}
