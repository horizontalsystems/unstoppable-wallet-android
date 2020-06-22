package io.horizontalsystems.bankwallet.modules.ratechart

import java.math.BigDecimal

object CoinInfoMap {
    
    val data = mapOf(
            "BTC" to CoinInfo(BigDecimal(21_000_000), "03/01/2009", "https://bitcoin.org/en/"),
            "LTC" to CoinInfo(BigDecimal(84_000_000),  "13/10/2011", "https://litecoin.com/"),
            "ETH" to CoinInfo( null,  "30/07/2015", "https://www.ethereum.org/"),
            "BCH" to CoinInfo(BigDecimal(21_000_000), "01/08/2017", "https://www.bitcoincash.org/"),
            "DASH" to CoinInfo(BigDecimal(18_900_000), "18/01/2014", "http://dash.org/"),
            "BNB" to CoinInfo(BigDecimal(187_536_713), "27/06/2017", "https://www.binance.com/"),
            "EOS" to CoinInfo(BigDecimal(1_035_000_004), "26/06/2017", "https://eos.io/"),
            "CVC" to CoinInfo(BigDecimal(1_000_000_000), "21/06/2017", "https://www.civic.com/"),
            "DNT" to CoinInfo(BigDecimal(1_000_000_000), "08/08/2017", "https://district0x.io/"),
            "ZRX" to CoinInfo(BigDecimal(1_000_000_000), "15/08/2017", "https://www.0xproject.com/#home"),
            "ELF" to CoinInfo(BigDecimal(880_000_000), "18/12/2017", "http://aelf.io/"),
            "ANKR" to CoinInfo(BigDecimal(10_000_000_000), "21/02/2019", "https://www.ankr.com/"),
            "ANT" to CoinInfo(BigDecimal(39_609_524), "05/05/2017", "https://aragon.one/"),
            "BNT" to CoinInfo(BigDecimal(67_721_371), "13/02/2017", "https://bancor.network/"),
            "BAT" to CoinInfo(BigDecimal(1_500_000_000), "31/05/2017", "https://basicattentiontoken.org/"),
            "BUSD" to CoinInfo(BigDecimal(28_603_822), "10/09/2019", "https://www.paxos.com/busd/"),
            "CAS" to CoinInfo(BigDecimal(1_000_000_000), "12/10/2017", "https://cashaa.com/"),
            "CHSB" to CoinInfo(BigDecimal(1_000_000_000), "08/09/2017", "https://swissborg.com/"),
            "LINK" to CoinInfo(BigDecimal(1_000_000_000), "19/09/2017", "https://link.smartcontract.com/"),
            "CRPT" to CoinInfo(BigDecimal(99_785_291), "28/09/2017", "https://crypterium.io/"),
            "CRO" to CoinInfo(BigDecimal(100_000_000_000), "14/11/2019", "https://www.crypto.com/en/chain"),
            "MANA" to CoinInfo(BigDecimal(2_644_403_343), "08/08/2017", "https://decentraland.org/"),
            "DGD" to CoinInfo(BigDecimal(2_000_000), "28/04/2016", "https://www.dgx.io/"),
            "ENJ" to CoinInfo(BigDecimal(1_000_000_000), "24/07/2017", "https://enjincoin.io/"),
            "IQ" to CoinInfo(BigDecimal(10_006_128_771), "14/07/2018", "https://everipedia.org/"),
            "GTO" to CoinInfo(BigDecimal(1_000_000_000), "14/12/2017", "https://gifto.io/"),
            "GNT" to CoinInfo(BigDecimal(1_000_000_000), "17/11/2016", "https://golem.network/"),
            "HOT" to CoinInfo(BigDecimal(177_619_433_541), "16/01/2018", "https://thehydrofoundation.com/"),
            "HT" to CoinInfo(BigDecimal(500_000_000), "22/01/2018", "https://www.huobi.pro/"),
            "IDEX" to CoinInfo(BigDecimal(1_000_000_000), "28/09/2017", "https://auroradao.com/"),
            "KCS" to CoinInfo(BigDecimal(176_863_551), "15/09/2017", "https://www.kucoin.com/#/"),
            "LOOM" to CoinInfo(BigDecimal(1_000_000_000), "03/03/2018", "https://loomx.io/"),
            "MKR" to CoinInfo(BigDecimal(1_000_000), "15/08/2015", null),
            "MEETONE" to CoinInfo(BigDecimal(10_000_000_000), "05/05/2018", "https://meet.one/"),
            "MITH" to CoinInfo(BigDecimal(1_000_000_000), "12/03/2018", "https://mith.io/"),
            "NDX" to CoinInfo(BigDecimal(10_000_000_000), null, null),
            "NEXO" to CoinInfo(BigDecimal(1_000_000_000), "29/04/2018", "https://nexo.io/"),
            "ORBS" to CoinInfo(BigDecimal(10_000_000_000), "14/03/2018", "https://www.orbs.com/"),
            "OXT" to CoinInfo(BigDecimal(10_000_000_000), "03/12/2019", "https://www.orchid.com/"),
            "PAXG" to CoinInfo(BigDecimal(2_410), "29/08/2019", "https://www.paxos.com/paxgold/"),
            "PPT" to CoinInfo(BigDecimal(53_252_246), "12/04/2017", "https://populous.world/"),
            "PTI" to CoinInfo(BigDecimal(3_600_000_000), "13/03/2018", "https://tokensale.paytomat.com/"),
            "POLY" to CoinInfo(BigDecimal(1_000_000_000), "25/12/2017", "https://www.polymath.network/"),
            "PGL" to CoinInfo(BigDecimal(220_000_000), "19/04/2017", "https://prospectors.io/en"),
            "NPXS" to CoinInfo(BigDecimal(259_810_708_833), "27/09/2017", "https://pundix.com/"),
            "R" to CoinInfo(BigDecimal(1_000_000_000), "04/08/2017", "http://revain.org/"),
            "SNT" to CoinInfo(BigDecimal(6_804_870_174), "20/06/2017", "https://status.im/"),
            "SNX" to CoinInfo(BigDecimal(174_648_076), "07/01/2018", "https://www.synthetix.io/"),
            "WTC" to CoinInfo(BigDecimal(70_000_000), "27/08/2017", "http://www.waltonchain.org/")
    )
}

data class CoinInfo(val supply: BigDecimal?, val startDate: String?, val website: String?)
