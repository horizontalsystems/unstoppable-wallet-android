package io.horizontalsystems.bankwallet.modules.nav3

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor() : ViewModel() {

    val uuid = UUID.randomUUID().toString()
}
