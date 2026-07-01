package io.horizontalsystems.bankwallet.core.managers

import android.util.Base64
import io.horizontalsystems.bankwallet.modules.backuplocal.BackupLocalModule
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import org.bouncycastle.crypto.generators.SCrypt
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptDecryptManager {

    private val cipher: Cipher = Cipher.getInstance(TRANSFORMATION_SYMMETRIC)

    fun encrypt(value: ByteArray, key: ByteArray, iv: String): String {
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv.hexStringToByteArray())
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(value)
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decrypt(value: String, key: ByteArray, iv: String): ByteArray {
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv.hexStringToByteArray())
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decoded = Base64.decode(value, Base64.NO_WRAP)
        return cipher.doFinal(decoded)
    }

    companion object{
        const val TRANSFORMATION_SYMMETRIC = "AES/CTR/NoPadding"

        /***
        val salt = byteArrayOf() //the salt to use for this invocation
        val n = 16384 //CPU/Memory cost parameter. Must be larger than 1, a power of 2 and less than 2^(128 * r / 8).
        val r = 8 //the block size, must be >= 1.
        val p = 8 //the bytes of the pass phrase
        val dkLen = 32 // Desired key length in bytes
         */
        fun getKey(password: String, n: Int, r: Int, p: Int, dkLen: Int, salt: ByteArray): ByteArray? {
            return try {
                val passwordByteArray = password.toByteArray(Charsets.UTF_8)
                SCrypt.generate(passwordByteArray, salt, n, r, p, dkLen)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun getKey(password: String, kdfParams: BackupLocalModule.KdfParams) : ByteArray?{
            return getKey(
                password,
                kdfParams.n,
                kdfParams.r,
                kdfParams.p,
                kdfParams.dklen,
                kdfParams.salt.toByteArray()
            )
        }

        fun generateMac(derivedKey: ByteArray, cipherText: ByteArray): ByteArray {
            val result = ByteArray(16 + cipherText.size)
            System.arraycopy(derivedKey, 16, result, 0, 16)
            System.arraycopy(cipherText, 0, result, 16, cipherText.size)
            return CryptoUtils.sha3(result) //get Keccak Hash
        }

        fun generateRandomBytes(number: Int): ByteArray {
            val random = SecureRandom()
            val bytes = ByteArray(number)
            random.nextBytes(bytes)
            return bytes
        }

        fun passwordIsCorrect(mac: String, cipherText: String, key: ByteArray): Boolean {
            return try {
                // Compute MAC using the same algorithm used during encryption
                val computedMac = EncryptDecryptManager.generateMac(key, cipherText.toByteArray())
                // Compare the computed MAC with the received MAC
                MessageDigest.isEqual(computedMac, mac.hexStringToByteArray())
            } catch (e: Exception) {
                false
            }
        }
    }

}
