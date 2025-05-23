package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@Stable
class Colors(
    jacob: Color,
    remus: Color,
    lucian: Color,
    tyler: Color,
    leah: Color,
    lawrence: Color,
    laguna: Color,
    raina: Color,
    andy: Color,
    blade: Color,
) {

    //base colors
    val transparent = Color.Transparent
    val dark = Dark
    val light = Light
    val white = Color.White
    val black50 = Black50
    val issykBlue = Color(0xFF3372FF)
    val lightGrey = LightGrey
    val grey = Grey
    val yellow50 = Yellow50
    val yellow20 = Yellow20
    val green20 = Green20

    val yellowD = YellowD
    val yellowL = YellowL
    val greenD = GreenD
    val greenL = GreenL
    val green50 = Green50
    val redD = RedD
    val redL = RedL
    val elenaD = Color(0xFF6E7899)
    val red50 = Red50
    val red20 = Red20

    //themed colors
    var jacob by mutableStateOf(jacob)
        private set
    var remus by mutableStateOf(remus)
        private set
    var lucian by mutableStateOf(lucian)
        private set
    var tyler by mutableStateOf(tyler)
        private set
    var leah by mutableStateOf(leah)
        private set
    var lawrence by mutableStateOf(lawrence)
        private set
    var laguna by mutableStateOf(laguna)
        private set
    var raina by mutableStateOf(raina)
        private set
    var andy by mutableStateOf(andy)
        private set
    var blade by mutableStateOf(blade)
        private set

    fun update(other: Colors) {
        jacob = other.jacob
        remus = other.remus
        lucian = other.lucian
        tyler = other.tyler
        leah = other.leah
        lawrence = other.lawrence
        laguna = other.laguna
        raina = other.raina
        andy = other.andy
        blade = other.blade
    }

    fun copy(): Colors = Colors(
        jacob = jacob,
        remus = remus,
        lucian = lucian,
        tyler = tyler,
        leah = leah,
        lawrence = lawrence,
        laguna = laguna,
        raina = raina,
        andy = andy,
        blade = blade,
    )
}
