package io.horizontalsystems.bankwallet.modules.importcexaccount

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaException
import com.hcaptcha.sdk.HCaptchaTokenResponse
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity

class HCaptchaActivity : BaseActivity() {
    private val hCaptcha = HCaptcha.getClient(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hCaptcha.setup(App.appConfigProvider.coinzixHCaptchaSiteKey).verifyWithHCaptcha()
        hCaptcha.addOnSuccessListener { response: HCaptchaTokenResponse ->
            val userResponseToken = response.tokenResult
            val intent = Intent()
            intent.putExtra("captcha", userResponseToken)
            setResult(RESULT_OK, intent)
            finish()
        }.addOnFailureListener { e: HCaptchaException ->
            // Error handling here: trigger another verification, display a toast, etc.
            Log.d("hCaptcha", "hCaptcha failed: " + e.message + "(" + e.statusCode + ")")
            setResult(RESULT_CANCELED)
            finish()
        }.addOnOpenListener {
            // Usefull for analytics purposes
            Log.d("hCaptcha", "hCaptcha is now visible.")
        }
    }
}