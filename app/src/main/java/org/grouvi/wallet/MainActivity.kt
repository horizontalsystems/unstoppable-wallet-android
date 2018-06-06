package org.grouvi.wallet

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.wallet.DeterministicSeed
import org.grouvi.wallet.modules.generateMnemonic.GenerateMnemonicActivity
import java.security.SecureRandom


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button_create_wallet.setOnClickListener {
            val intent = Intent(this, GenerateMnemonicActivity::class.java)
            startActivity(intent)
        }
    }


    private fun createNewWallet() {

        val xxx = XXX()
        xxx.generateSeed("")
        xxx.createMasterKey()
        xxx.deriveBip44RootKey()
        xxx.deriveBitcoinRootKey()







//        dkRoot = HDKeyDerivation.deriveChildKey(dKey, ChildNumber.HARDENED_BIT)




    }


}

class XXX {
    private var seed: DeterministicSeed? = null
    private var masterKey: DeterministicKey? = null
    private var bip44RootKey: DeterministicKey? = null
    private var bitcoinRootKey: DeterministicKey? = null

    fun generateSeed(passphrase: String) {
        seed = DeterministicSeed(SecureRandom(), DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS, passphrase, Utils.currentTimeSeconds())

        seed?.mnemonicCode.log("Mnemonic Code")

    }

    fun createMasterKey() {
        masterKey = HDKeyDerivation.createMasterPrivateKey(seed?.seedBytes)
    }

    fun deriveBip44RootKey() {
        bip44RootKey = HDKeyDerivation.deriveChildKey(masterKey, ChildNumber(44, true))
    }

    fun deriveBitcoinRootKey() {
        bitcoinRootKey = HDKeyDerivation.deriveChildKey(bip44RootKey, ChildNumber(0, true))
    }

}

fun Any?.log(label: String = "") {

    Log.e("AAA", "$label: $this")
}