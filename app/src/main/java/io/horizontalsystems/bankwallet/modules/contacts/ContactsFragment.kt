package io.horizontalsystems.bankwallet.modules.contacts

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import kotlinx.parcelize.Parcelize

class ContactsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
    }

    @Parcelize
    data class Input(val mode: Mode) : Parcelable
}
