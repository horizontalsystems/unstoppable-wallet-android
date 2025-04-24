package io.horizontalsystems.stellarkit

import org.stellar.sdk.Network

enum class Network {
    MainNet, TestNet;

    fun toStellarNetwork() = when (this) {
        MainNet -> Network.PUBLIC
        TestNet -> Network.TESTNET
    }
}