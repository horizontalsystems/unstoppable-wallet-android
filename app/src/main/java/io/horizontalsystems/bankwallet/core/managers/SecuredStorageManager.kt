package io.horizontalsystems.bankwallet.core.managers

import android.text.TextUtils
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.Account
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IEncryptionManager
import io.horizontalsystems.bankwallet.core.ISecuredStorage
import io.horizontalsystems.bankwallet.core.utils.JsonUtils
import io.horizontalsystems.bankwallet.entities.AuthData


class SecuredStorageManager(private val encryptionManager: IEncryptionManager) : ISecuredStorage {

    private val AUTH_DATA = "auth_data"
    private val LOCK_PIN = "lock_pin"
    private val ACCOUNTS = "accounts"


    override val authData: AuthData?
        get() {
            App.preferences.getString(AUTH_DATA, null)?.let { string ->
                return AuthData(encryptionManager.decrypt(string))
            }
            return null
        }

    override fun saveAuthData(authData: AuthData) {
        App.preferences.edit().putString(AUTH_DATA, encryptionManager.encrypt(authData.toString())).apply()
    }

    override fun noAuthData(): Boolean {
        val words = App.preferences.getString(AUTH_DATA, null)
        return words.isNullOrEmpty()
    }

    override val savedPin: String?
        get() {
            val string = App.preferences.getString(LOCK_PIN, null)
            return if (TextUtils.isEmpty(string)) {
                null
            } else {
                encryptionManager.decrypt(string)
            }
        }

    override fun savePin(pin: String) {
        App.preferences.edit().putString(LOCK_PIN, encryptionManager.encrypt(pin)).apply()
    }

    override fun pinIsEmpty(): Boolean {
        val string = App.preferences.getString(LOCK_PIN, null)
        return string.isNullOrEmpty()
    }

    override val accounts: List<Account>
        get() {
            val accountsEncrypted = App.preferences.getString(ACCOUNTS, null) ?: return listOf()
            val accountsJson = encryptionManager.decrypt(accountsEncrypted)

            return JsonUtils.gson.fromJson(accountsJson, object : TypeToken<List<Account>>() {}.type)
        }

    override fun saveAccounts(accounts: List<Account>) {
        val accountsJson = JsonUtils.gson.toJson(accounts)
        val accountsEncrypted = encryptionManager.encrypt(accountsJson)

        App.preferences.edit().putString(ACCOUNTS, accountsEncrypted).apply()
    }

}
