package io.horizontalsystems.bankwallet.modules.swap

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin

class SwapViewModel(
        var tokenIn: Coin? = null,
        var tokenOut: Coin? = null
) : ViewModel() {


}
