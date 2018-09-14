package bitcoin.wallet.core.security

import android.app.KeyguardManager
import android.content.Context
import android.content.Context.FINGERPRINT_SERVICE
import android.hardware.fingerprint.FingerprintManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.security.spec.InvalidKeySpecException
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource


object SecurityUtils {

    private val KEY_ALIAS = "FINGERPRINT_KEY_PAIR_ALIAS"
    private val KEY_STORE = "AndroidKeyStore"

    private var sKeyStore: KeyStore? = null
    private var sKeyPairGenerator: KeyPairGenerator? = null
    private var sCipher: Cipher? = null

    fun touchSensorCanBeUsed(context: Context): Boolean {
        val fingerprintManager = context.getSystemService(FINGERPRINT_SERVICE) as? FingerprintManager
        if (fingerprintManager?.isHardwareDetected == true) {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            return keyguardManager.isKeyguardSecure && fingerprintManager.hasEnrolledFingerprints()
        } else
            return false
    }

    fun phoneHasFingerprintSensor(context: Context): Boolean {
        val fingerprintManager = context.getSystemService(FINGERPRINT_SERVICE) as? FingerprintManager
        return fingerprintManager?.isHardwareDetected ?: false
    }

    fun getCryptoObject(): FingerprintManager.CryptoObject? {
        return if (initKeyStore() && initCipher() && initKey() && initCipherMode(Cipher.DECRYPT_MODE)) {
            FingerprintManager.CryptoObject(sCipher)
        } else null
    }

    private fun initKeyStore(): Boolean {
        try {
            sKeyStore = KeyStore.getInstance(KEY_STORE)
            sKeyStore?.load(null)
            return true
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        }

        return false
    }

    private fun initCipher(): Boolean {
        try {
            sCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            return true
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        }

        return false
    }

    private fun initCipherMode(mode: Int): Boolean {
        try {
            sKeyStore?.load(null)
            when (mode) {
                Cipher.ENCRYPT_MODE -> {
                    val key = sKeyStore?.getCertificate(KEY_ALIAS)?.publicKey
                    val unrestricted = KeyFactory.getInstance(key?.algorithm).generatePublic(X509EncodedKeySpec(key?.encoded))
                    val spec = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT)
                    sCipher?.init(mode, unrestricted, spec)
                }

                Cipher.DECRYPT_MODE -> {
                    val privateKey = sKeyStore?.getKey(KEY_ALIAS, null) as PrivateKey
                    sCipher?.init(mode, privateKey)
                }
                else -> return false
            }
            return true
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        } catch (e: CertificateException) {
            e.printStackTrace()
        } catch (e: UnrecoverableKeyException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: InvalidAlgorithmParameterException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        }

        return false
    }

    private fun initKey(): Boolean {
        try {
            return sKeyStore?.containsAlias(KEY_ALIAS) == true || generateNewKey()
        } catch (e: KeyStoreException) {
            e.printStackTrace()
        }

        return false
    }

    private fun generateNewKey(): Boolean {
        if (initKeyGenerator()) {
            try {
                sKeyPairGenerator?.initialize(KeyGenParameterSpec.Builder(KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .setUserAuthenticationRequired(true)
                        .build())
                sKeyPairGenerator?.generateKeyPair()
                return true
            } catch (e: InvalidAlgorithmParameterException) {
                e.printStackTrace()
            }

        }
        return false
    }

    private fun initKeyGenerator(): Boolean {
        try {
            sKeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEY_STORE)
            return true
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: NoSuchProviderException) {
            e.printStackTrace()
        }

        return false
    }
}
