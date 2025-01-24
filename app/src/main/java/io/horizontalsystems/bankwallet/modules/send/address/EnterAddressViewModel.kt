package io.horizontalsystems.bankwallet.modules.send.address

import android.util.Log
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.Address

class EnterAddressViewModel : ViewModelUiState<EnterAddressUiState>() {
    private var address: Address? = null
    private var addressError: Throwable? = null
    private var canBeSendToAddress: Boolean = false
    private var recentAddress: String? = "1EzZFZhopU4vBJKLiq5kUKphXZ4M22M1QjmEdfk"
    private val contacts = listOf(
        SContact("My Wallet", "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"),
        SContact("TalgatETH", "0x95222290DD7278Aa3Ddd389Cc1E1d165CC4BAfe5"),
        SContact("Esso", "0x71C7656EC7ab88b098defB751B7401B5f6d8976F"),
        SContact("Escobar", "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF"),
        SContact("Vitalik", "0xAb5801a7D398351b8bE11C439e05C5B3259aeC9B"),
        SContact("Binance_1", "0x28C6c06298d514Db089934071355E5743bf21d60"),
        SContact("Kraken_Hot", "0x2910543Af39abA0Cd09dBb2D50200b3E800A63D2"),
        SContact("FTX_Exploiter", "0x59ABf3837Fa962d6853b4Cc0a19513AA031fd32b"),
        SContact("Coinbase_2", "0x503828976D22510aad0201ac7EC88293211D23Da"),
        SContact("Metamask_DEV", "0x9696f59E4d72E237BE84fFD425DCaD154Bf96976")
    )

    override fun createState() = EnterAddressUiState(
        address = address,
        addressError = addressError,
        canBeSendToAddress = canBeSendToAddress,
        recentAddress = recentAddress,
        contacts = contacts
    )

    init {

    }

    fun onEnterAddress(address: Address?) {
        this.address = address
        this.canBeSendToAddress = address != null

        emitState()

        Log.e("AAA", "onEnterAddress: $address")
//        TODO("Not yet implemented")
    }
}

data class EnterAddressUiState(
    val address: Address?,
    val addressError: Throwable?,
    val canBeSendToAddress: Boolean,
    val recentAddress: String?,
    val contacts: List<SContact>,
)
