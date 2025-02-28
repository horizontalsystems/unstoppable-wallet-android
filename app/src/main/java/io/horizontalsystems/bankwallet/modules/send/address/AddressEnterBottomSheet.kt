package io.horizontalsystems.bankwallet.modules.send.address

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.address.AddressCheckType
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.highlightText
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressEnterInfoBottomSheet(
    checkType: AddressCheckType,
    hideBottomSheet: () -> Unit,
    bottomSheetState: SheetState
) {
    val title = when (checkType) {
        AddressCheckType.Phishing -> R.string.Send_Address_PhishingCheck
        AddressCheckType.Blacklist -> R.string.Send_Address_BlacklistCheck
        AddressCheckType.Sanction -> R.string.Send_Address_SanctionCheck
    }

    ModalBottomSheet(
        onDismissRequest = hideBottomSheet,
        sheetState = bottomSheetState,
        containerColor = ComposeAppTheme.colors.transparent
    ) {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_info_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey),
            title = stringResource(title),
            titleColor = ComposeAppTheme.colors.leah,
            onCloseClick = hideBottomSheet
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                InfoBlock(checkType)
                VSpacer(36.dp)
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Close),
                    onClick = hideBottomSheet
                )
                VSpacer(32.dp)
            }
        }
    }
}

@Composable
fun InfoBlock(checkType: AddressCheckType) {
    val info1 = when (checkType) {
        AddressCheckType.Phishing -> AnnotatedString(stringResource(R.string.Send_Address_PhishingCheck_Info1))

        AddressCheckType.Blacklist -> highlightText(
            text = stringResource(R.string.Send_Address_BlacklistCheck_Info1),
            textColor = ComposeAppTheme.colors.leah,
            highlightPart = "Hashdit.io",
            highlightColor = ComposeAppTheme.colors.jacob
        )

        AddressCheckType.Sanction -> highlightText(
            text = stringResource(R.string.Send_Address_SanctionCheck_Info1),
            textColor = ComposeAppTheme.colors.leah,
            highlightPart = "Chainalysis.com",
            highlightColor = ComposeAppTheme.colors.jacob
        )
    }

    val info2 = when (checkType) {
        AddressCheckType.Phishing -> highlightText(
            text = stringResource(R.string.Send_Address_PhishingCheck_Info2),
            textColor = ComposeAppTheme.colors.leah,
            highlightPart = stringResource(R.string.Send_Address_Error_Clear),
            highlightColor = ComposeAppTheme.colors.remus
        )

        AddressCheckType.Blacklist -> highlightText(
            text = stringResource(R.string.Send_Address_BlacklistCheck_Info2),
            textColor = ComposeAppTheme.colors.leah,
            highlightPart = stringResource(R.string.Send_Address_Error_Clear),
            highlightColor = ComposeAppTheme.colors.remus
        )

        AddressCheckType.Sanction -> highlightText(
            text = stringResource(R.string.Send_Address_SanctionCheck_Info2),
            textColor = ComposeAppTheme.colors.leah,
            highlightPart = stringResource(R.string.Send_Address_Error_Clear),
            highlightColor = ComposeAppTheme.colors.remus
        )
    }

    val info3 = when (checkType) {
        AddressCheckType.Phishing -> highlightText(
            text = stringResource(R.string.Send_Address_PhishingCheck_Info3),
            textColor = ComposeAppTheme.colors.leah,
            highlightPart = stringResource(R.string.Send_Address_Error_Detected),
            highlightColor = ComposeAppTheme.colors.lucian
        )

        AddressCheckType.Blacklist -> highlightText(
            text = stringResource(R.string.Send_Address_BlacklistCheck_Info3),
            textColor = ComposeAppTheme.colors.leah,
            highlightPart = stringResource(R.string.Send_Address_Error_Detected),
            highlightColor = ComposeAppTheme.colors.lucian
        )

        AddressCheckType.Sanction -> highlightText(
            text = stringResource(R.string.Send_Address_SanctionCheck_Info3),
            textColor = ComposeAppTheme.colors.leah,
            highlightPart = stringResource(R.string.Send_Address_Error_Detected),
            highlightColor = ComposeAppTheme.colors.lucian
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = info1,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
        )
        VSpacer(24.dp)
        Text(
            text = info2,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
        )
        Text(
            text = info3,
            style = ComposeAppTheme.typography.body,
            color = ComposeAppTheme.colors.leah,
        )
    }
}
