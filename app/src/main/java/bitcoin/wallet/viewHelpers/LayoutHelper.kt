package bitcoin.wallet.viewHelpers

import android.graphics.PorterDuff
import android.view.Menu

object LayoutHelper {

    fun tintMenuIcons(menu: Menu, color: Int ) {
        for (i in 0 until menu.size()) {
            val drawable = menu.getItem(i).icon
            if (drawable != null) {
                drawable.mutate()
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

}
