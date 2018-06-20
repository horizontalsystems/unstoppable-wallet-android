package bitcoin.wallet.modules.main

import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity

abstract class BaseTabFragment : Fragment() {

    abstract val title: Int

    var active: Boolean = true
        set(value) {
            if (value) {
                (activity as? AppCompatActivity)?.supportActionBar?.setTitle(title)
            }
        }

}
