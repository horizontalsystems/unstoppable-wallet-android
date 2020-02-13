package io.horizontalsystems.core.security

import android.util.Base64
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

class CipherWrapper {

    companion object {
        const val TRANSFORMATION_SYMMETRIC = "AES/CBC/PKCS7Padding"
        const val IV_SEPARATOR = "]"
    }

    val cipher: Cipher = Cipher.getInstance(TRANSFORMATION_SYMMETRIC)

    fun encrypt(data: String, key: Key): String {
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ivString = Base64.encodeToString(iv, Base64.DEFAULT)
        var result = ivString + IV_SEPARATOR

        val bytes = cipher.doFinal(data.toByteArray())
        result += Base64.encodeToString(bytes, Base64.DEFAULT)

        return result
    }

    fun decrypt(data: String, key: Key): String {
        val split = data.split(IV_SEPARATOR.toRegex())
        if (split.size != 2) throw IllegalArgumentException("Passed data is incorrect. There was no IV specified with it.")

        val ivString = split[0]
        val encodedString = split[1]
        val ivSpec = IvParameterSpec(Base64.decode(ivString, Base64.DEFAULT))
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

        val encryptedData = Base64.decode(encodedString, Base64.DEFAULT)
        val decodedData = cipher.doFinal(encryptedData)
        return String(decodedData)
    }

}
