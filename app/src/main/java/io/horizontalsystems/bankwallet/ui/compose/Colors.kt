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
    bran: Color,
    leah: Color,
    claude: Color,
    lawrence: Color,
    jeremy: Color,
    laguna: Color,
    purple: Color,
    raina: Color,
    andy: Color,
) {

    //base colors
    val transparent = Color.Transparent
    val dark = Dark
    val light = Light
    val white = Color.White
    val black50 = Black50
    val issykBlue = Color(0xFF3372FF)
    val lightGrey = LightGrey
    val steelLight = SteelLight
    val steelDark = SteelDark
    val steel10 = Steel10
    val steel20 = Steel20
    val grey = Grey
    val grey50 = Grey50
    val yellow50 = Yellow50
    val yellow20 = Yellow20

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
    var bran by mutableStateOf(bran)
        private set
    var leah by mutableStateOf(leah)
        private set
    var claude by mutableStateOf(claude)
        private set
    var lawrence by mutableStateOf(lawrence)
        private set
    var jeremy by mutableStateOf(jeremy)
        private set
    var laguna by mutableStateOf(laguna)
        private set
    var purple by mutableStateOf(purple)
        private set
    var raina by mutableStateOf(raina)
        private set
    var andy by mutableStateOf(andy)
        private set

    fun update(other: Colors) {
        jacob = other.jacob
        remus = other.remus
        lucian = other.lucian
        tyler = other.tyler
        bran = other.bran
        leah = other.leah
        claude = other.claude
        lawrence = other.lawrence
        jeremy = other.jeremy
        laguna = other.laguna
        purple = other.purple
        raina = other.raina
        andy = other.andy
    }

    fun copy(): Colors = Colors(
        jacob = jacob,
        remus = remus,
        lucian = lucian,
        tyler = tyler,
        bran = bran,
        leah = leah,
        claude = claude,
        lawrence = lawrence,
        jeremy = jeremy,
        laguna = laguna,
        purple = purple,
        raina = raina,
        andy = andy,
    )
}
