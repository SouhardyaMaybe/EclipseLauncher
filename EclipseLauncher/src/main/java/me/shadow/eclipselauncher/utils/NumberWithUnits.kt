package me.shadow.eclipselauncher.utils

import me.shadow.eclipselauncher.utils.stringutils.StringUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class NumberWithUnits {
    companion object {
        private val UNITS_EN = arrayOf("", "K", "M") //English units: thousand, million

        @JvmStatic
        fun formatNumberWithUnit(number: Long): String {
            return formatNumber(number, 1000, UNITS_EN)
        }

        private fun formatNumber(number: Long, stage: Int, units: Array<String>): String {
            var bigDecimal = BigDecimal(number)
            var unitIndex = 0

            while (bigDecimal >= BigDecimal.valueOf(stage.toLong()) && unitIndex < units.size - 1) {
                bigDecimal = bigDecimal.divide(BigDecimal.valueOf(stage.toLong()), 2, RoundingMode.DOWN)
                unitIndex++
            }

            //检查是否为空的单位，如果是，那么就不做格式化，直接返回原始值
            if (units[unitIndex].isEmpty()) {
                return number.toString()
            } else {
                val df = DecimalFormat("#.00")
                val formattedNumber = df.format(bigDecimal.setScale(2, RoundingMode.DOWN).toDouble())
                return StringUtils.insertSpace(formattedNumber, units[unitIndex])
            }
        }
    }
}
