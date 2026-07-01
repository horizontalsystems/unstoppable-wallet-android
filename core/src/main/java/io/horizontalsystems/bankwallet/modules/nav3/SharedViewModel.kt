package io.horizontalsystems.bankwallet.modules.nav3

import androidx.lifecycle.ViewModel
import java.util.UUID

class SharedViewModel : ViewModel() {

    val uuid = UUID.randomUUID().toString()

}
