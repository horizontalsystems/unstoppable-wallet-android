package io.horizontalsystems.chartview

import io.horizontalsystems.chartview.models.ChartPointF
import org.junit.Test

class XxxTest {

    @Test
    fun test() {

        val prevPointsMap = mapOf(
            1645056000L to ChartPointF(24.161345f, 359.54178f),
            1645084800L to ChartPointF(33.984577f, 355.1588f),
//            1645113600L to ChartPointF(58.605026f, 360.29004f),
//            1645142400L to ChartPointF(83.225464f, 373.98492f),
//            1645171200L to ChartPointF(107.8459f, 366.81317f),
//            1645200000L to ChartPointF(132.46634f, 358.55133f),
//            1645228800L to ChartPointF(157.08676f, 339.76624f),
//            1645257600L to ChartPointF(181.70723f, 317.53772f),
//            1645286400L to ChartPointF(206.32765f, 308.05646f),
//            1645315200L to ChartPointF(230.9481f, 309.03558f),
//            1645344000L to ChartPointF(255.56851f, 330.8569f),
//            1645372800L to ChartPointF(280.18896f, 352.75787f),
//            1645401600L to ChartPointF(304.80942f, 346.21814f),
//            1645430400L to ChartPointF(329.42984f, 331.35464f),
//            1645459200L to ChartPointF(354.0503f, 328.35886f),
//            1645488000L to ChartPointF(378.67075f, 325.42935f),
//            1645516800L to ChartPointF(403.29117f, 315.92593f),
//            1645545600L to ChartPointF(427.91162f, 285.71017f),
//            1645574400L to ChartPointF(452.53207f, 272.08115f),
//            1645603200L to ChartPointF(477.1525f, 262.22437f),
//            1645617600L to ChartPointF(489.46268f, 270.55396f),
//            1645632000L to ChartPointF(501.77292f, 278.37177f),
//            1645639200L to ChartPointF(507.92798f, 276.9886f),
//            1645646400L to ChartPointF(514.0831f, 275.60538f),
//            1645660800L to ChartPointF(526.3934f, 273.2533f),
//            1645675200L to ChartPointF(538.70355f, 283.2534f),
//            1645682400L to ChartPointF(544.8587f, 288.20575f),
//            1645689600L to ChartPointF(551.0138f, 293.15808f),
//            1645704000L to ChartPointF(563.32404f, 279.62167f),
//            1645718400L to ChartPointF(575.6343f, 266.5235f),
//            1645725600L to ChartPointF(581.7893f, 255.39038f),
//            1645732800L to ChartPointF(587.94446f, 244.2571f),
//            1645747200L to ChartPointF(600.2547f, 221.93788f),
//            1645761600L to ChartPointF(612.5649f, 218.65233f),
//            1645768800L to ChartPointF(618.72f, 217.12488f),
//            1645776000L to ChartPointF(624.8751f, 215.59741f),
//            1645790400L to ChartPointF(637.18536f, 212.83351f),
//            1645804800L to ChartPointF(649.49554f, 210.40186f),
//            1645812000L to ChartPointF(655.65063f, 201.74525f),
//            1645819200L to ChartPointF(661.8058f, 193.08856f),
//            1645833600L to ChartPointF(674.11597f, 175.71451f),
//            1645848000L to ChartPointF(686.4262f, 176.09595f),
//            1645855200L to ChartPointF(692.5813f, 176.18396f),
//            1645862400L to ChartPointF(698.73645f, 176.27194f),
//            1645876800L to ChartPointF(711.04663f, 161.59984f),
//            1645891200L to ChartPointF(723.3569f, 146.81992f),
//            1645898400L to ChartPointF(729.51196f, 153.17923f),
//            1645905600L to ChartPointF(735.6671f, 159.53864f),
//            1645920000L to ChartPointF(747.9773f, 172.06389f),
//            1645934400L to ChartPointF(760.2875f, 174.49959f),
//            1645941600L to ChartPointF(766.4426f, 175.66959f),
//            1645948800L to ChartPointF(772.5978f, 176.83957f),
//            1645963200L to ChartPointF(784.90796f, 170.91133f),
//            1645977600L to ChartPointF(797.21826f, 165.1081f),
//            1645984800L to ChartPointF(803.3733f, 169.21225f),
//            1645992000L to ChartPointF(809.5284f, 173.31647f),
//            1646006400L to ChartPointF(821.8386f, 181.62575f),
//            1646020800L to ChartPointF(834.1488f, 186.20538f),
//            1646028000L to ChartPointF(840.30396f, 188.24632f),
//            1646035200L to ChartPointF(846.4591f, 190.28728f),
//            1646049600L to ChartPointF(858.7692f, 186.91719f),
//            1646064000L to ChartPointF(871.0795f, 182.93118f),
//            1646071200L to ChartPointF(877.23456f, 187.81612f),
//            1646078400L to ChartPointF(883.3897f, 192.70108f),
//            1646092800L to ChartPointF(895.69995f, 202.43288f),
//            1646107200L to ChartPointF(908.01013f, 202.41959f),
//            1646114400L to ChartPointF(914.1653f, 202.38821f),
//            1646121600L to ChartPointF(920.32043f, 202.35684f),
//            1646136000L to ChartPointF(932.63055f, 186.8782f),
//            1646150400L to ChartPointF(944.94086f, 170.62f),
//            1646157600L to ChartPointF(951.0959f, 171.34018f),
//            1646164800L to ChartPointF(957.25104f, 172.0604f),
//            1646179200L to ChartPointF(969.56134f, 172.82788f),
//            1646193600L to ChartPointF(981.87146f, 169.07599f),
//            1646200800L to ChartPointF(988.02655f, 167.22177f),
//            1646208000L to ChartPointF(994.18176f, 165.36755f),
//            1646222400L to ChartPointF(1006.4919f, 156.54784f),
//            1646236800L to ChartPointF(1018.8022f, 148.09511f),
//            1646244000L to ChartPointF(1024.9573f, 138.4778f),
//            1646251200L to ChartPointF(1031.1123f, 128.86044f),
//            1646265600L to ChartPointF(1043.4226f, 109.67944f),
//            1646280000L to ChartPointF(1055.7328f, 85.32183f),
//            1646287200L to ChartPointF(1061.888f, 73.09723f),
//            1646294400L to ChartPointF(1068.043f, 60.872585f),
//            1646308315L to ChartPointF(1079.9386f, 47.797077f),
        )

        val nextPointsMap = mapOf(
//            1643695200L to ChartPointF(0.0f, 351.6514f),
//            1643738400L to ChartPointF(17.854061f, 349.22183f),
//            1643781600L to ChartPointF(35.708122f, 352.36295f),
//            1643824800L to ChartPointF(53.562183f, 356.7165f),
//            1643868000L to ChartPointF(71.416245f, 371.02402f),
//            1643911200L to ChartPointF(89.2703f, 372.25f),
//            1643954400L to ChartPointF(107.12437f, 370.03186f),
//            1643997600L to ChartPointF(124.978424f, 364.916f),
//            1644040800L to ChartPointF(142.83249f, 361.70926f),
//            1644084000L to ChartPointF(160.68655f, 362.69946f),
//            1644127200L to ChartPointF(178.5406f, 362.685f),
//            1644170400L to ChartPointF(196.39467f, 363.89557f),
//            1644213600L to ChartPointF(214.24873f, 361.7026f),
//            1644256800L to ChartPointF(232.10278f, 352.93054f),
//            1644300000L to ChartPointF(249.95685f, 343.98907f),
//            1644343200L to ChartPointF(267.8109f, 351.0616f),
//            1644386400L to ChartPointF(285.66498f, 343.34003f),
//            1644429600L to ChartPointF(303.51904f, 336.2635f),
//            1644472800L to ChartPointF(321.3731f, 330.20853f),
//            1644516000L to ChartPointF(339.22717f, 322.0987f),
//            1644559200L to ChartPointF(357.0812f, 318.28217f),
//            1644602400L to ChartPointF(374.9353f, 320.99304f),
//            1644645600L to ChartPointF(392.78934f, 342.26727f),
//            1644688800L to ChartPointF(410.6434f, 343.23993f),
//            1644732000L to ChartPointF(428.49747f, 331.40726f),
//            1644775200L to ChartPointF(446.35153f, 319.4127f),
//            1644818400L to ChartPointF(464.20557f, 320.1471f),
//            1644861600L to ChartPointF(482.05966f, 297.8134f),
//            1644904800L to ChartPointF(499.9137f, 283.68417f),
//            1644948000L to ChartPointF(517.76776f, 288.97247f),
//            1644991200L to ChartPointF(535.6218f, 299.74567f),
//            1645034400L to ChartPointF(553.4759f, 303.057f),
            1645077600L to ChartPointF(571.32996f, 300.11072f),

//            1645120800L to ChartPointF(589.184f, 304.42725f),
//            1645164000L to ChartPointF(607.0381f, 308.21075f),
//            1645207200L to ChartPointF(624.8921f, 302.80103f),
//            1645250400L to ChartPointF(642.7462f, 273.42767f),
//            1645293600L to ChartPointF(660.6003f, 256.08167f),
//            1645336800L to ChartPointF(678.45435f, 270.3739f),
//            1645380000L to ChartPointF(696.30835f, 292.65695f),
//            1645423200L to ChartPointF(714.1624f, 283.04263f),
//            1645466400L to ChartPointF(732.0165f, 269.9729f),
//            1645509600L to ChartPointF(749.8706f, 269.95987f),
//            1645552800L to ChartPointF(767.7246f, 251.59674f),
//            1645596000L to ChartPointF(785.5787f, 230.05801f),
//            1645639200L to ChartPointF(803.43274f, 234.43979f),
//            1645682400L to ChartPointF(821.2868f, 245.34686f),
//            1645725600L to ChartPointF(839.14087f, 221.53273f),
//            1645768800L to ChartPointF(856.99493f, 182.16013f),
//            1645812000L to ChartPointF(874.849f, 181.5001f),
//            1645855200L to ChartPointF(892.70306f, 157.95566f),
//            1645898400L to ChartPointF(910.55707f, 141.66124f),
//            1645941600L to ChartPointF(928.41113f, 155.6857f),
//            1645984800L to ChartPointF(946.26526f, 143.18637f),
//            1646028000L to ChartPointF(964.1193f, 174.86758f),
//            1646071200L to ChartPointF(981.9733f, 162.74767f),
//            1646114400L to ChartPointF(999.8274f, 175.28305f),
//            1646157600L to ChartPointF(1017.68146f, 157.87215f),
//            1646200800L to ChartPointF(1035.5355f, 143.85281f),
//            1646244000L to ChartPointF(1053.3896f, 111.432526f),
//            1646287200L to ChartPointF(1071.2437f, 77.035645f),
//            1646308365L to ChartPointF(1079.991f, 47.75f),
        )

        val prevStartTimestamp = 1645037184L

        val filled = Yyy.fillWith(
            LinkedHashMap(prevPointsMap),
            LinkedHashMap(nextPointsMap),
            prevStartTimestamp,
            prevStartTimestamp,
        )

        var prevX: Float? = null
        filled.map { (t, p) ->
            val currentX = p.x

            prevX?.let {
                if (currentX < it) {
                    println("Yahoo: $t, currentX: $currentX, prevX: $it")
                }
            }



            prevX = currentX
        }
    }
}