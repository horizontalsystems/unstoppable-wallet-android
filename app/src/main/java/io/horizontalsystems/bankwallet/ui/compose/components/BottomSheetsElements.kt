package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

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
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
        )
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
fun BottomSheetsElementsInput(onValueChange: (String) -> Unit) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        FormsInput(
            hint = stringResource(R.string.Restore_ZCash_Birthday_Hint),
            pasteEnabled = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textPreprocessor = object : TextPreprocessor {
                override fun process(text: String): String {
                    return text.replace("[^0-9]".toRegex(), "")
                }
            },
            onValueChange = onValueChange
        )
    }
}

@Composable
fun BottomSheetsElementsText(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
        )

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

@Composable
fun BottomSheetsElementsCheckbox(
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var checked by remember { mutableStateOf(false) }
            HsCheckbox(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onCheckedChange.invoke(it)
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            subhead2_leah(text = stringResource(R.string.Restore_ZCash_RestoreAsNew))
        }
    }
}