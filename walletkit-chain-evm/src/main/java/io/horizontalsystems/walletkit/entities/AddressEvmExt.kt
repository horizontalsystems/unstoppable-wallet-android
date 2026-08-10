package io.horizontalsystems.walletkit.entities

fun Address?.getEthereumKitAddress(): io.horizontalsystems.ethereumkit.models.Address? {
    val hex = this?.hex ?: return null

    return try {
        io.horizontalsystems.ethereumkit.models.Address(hex)
    } catch (err: Exception) {
        null
    }
}
