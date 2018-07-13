package bitcoin.wallet.bitcoin

import bitcoin.wallet.log
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.wallet.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

class WalletBip44 {

    companion object {

        @Throws(UnreadableWalletException::class)
        fun loadFromFile(file: File, params: NetworkParameters, vararg walletExtensions: WalletExtension): Wallet {
            try {
                var stream: FileInputStream? = null
                try {
                    stream = FileInputStream(file)
                    return loadFromFileStream(stream, params, *walletExtensions)
                } finally {
                    stream?.close()
                }
            } catch (e: IOException) {
                throw UnreadableWalletException("Could not open file", e)
            }

        }

        @Throws(UnreadableWalletException::class)
        private fun loadFromFileStream(stream: InputStream, params: NetworkParameters, vararg walletExtensions: WalletExtension): Wallet {
            val walletProtobufSerializer = WalletProtobufSerializer()
            walletProtobufSerializer.setKeyChainFactory(Bip44KeyChainFactory(params))
            val wallet = walletProtobufSerializer.readWallet(stream, *walletExtensions)
            if (!wallet.isConsistent) {
                "Loaded an inconsistent wallet".log()
            }
            return wallet
        }

        fun newFromSeedCode(params: NetworkParameters, seedCode: String): Wallet {
            return Wallet(params, getKeyChainGroup(params, seedCode))
        }

        private fun getKeyChainGroup(params: NetworkParameters, seedCode: String): KeyChainGroup {
            // todo set creation time to now
            val creationTime = 1527940800L // test wallet "used ugly..." creation date

//            val creationTime = Date().time / 1000
            val seed = DeterministicSeed(seedCode, null, "", creationTime)

            val keyChainGroup = KeyChainGroup(params)

            val keyChain = when (params) {
                is TestNet3Params -> Bip44TestNetDeterministicKeyChain(seed)
                is MainNetParams -> Bip44DeterministicKeyChain(seed)
                else -> null
            }

            keyChain?.let { keyChainGroup.addAndActivateHDChain(it) }

            return keyChainGroup
        }

    }

}
