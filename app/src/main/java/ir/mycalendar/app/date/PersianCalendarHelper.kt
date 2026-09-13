package ir.mycalendar.app.date

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class PersianDate(
    val year: Int,
    val month: Int, // 1 to 12
    val day: Int,   // 1 to 31
    val dayOfWeek: Int // 1 = Shanbeh (Saturday) .. 7 = Jom'eh (Friday)
)

data class FullDateInfo(
    val persianYear: Int,
    val persianMonth: Int,
    val persianDay: Int,
    val persianMonthName: String,
    val persianDayOfWeekName: String,
    val persianFormatted: String,          // e.g. "۱۴۰۳/۰۶/۲۳"
    val persianFullTitle: String,          // e.g. "جمعه ۲۳ شهریور ۱۴۰۳"
    val gregorianYear: Int,
    val gregorianMonth: Int,
    val gregorianDay: Int,
    val gregorianMonthNamePersian: String, // e.g. "سپتامبر"
    val gregorianMonthNameEnglish: String, // e.g. "September"
    val gregorianDayOfWeekName: String,    // e.g. "Friday"
    val gregorianFormatted: String,        // e.g. "2024-09-13"
    val isPersianLeapYear: Boolean,
    val daysInPersianMonth: Int
)

object PersianCalendarHelper {

    val PERSIAN_MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val GREGORIAN_MONTH_NAMES_PERSIAN = listOf(
        "ژانویه", "فوریه", "مارس", "آوریل",
        "مه", "ژوئن", "ژوئیه", "اوت",
        "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
    )

    val GREGORIAN_MONTH_NAMES_ENGLISH = listOf(
        "January", "February", "March", "April",
        "May", "June", "July", "August",
        "September", "October", "November", "December"
    )

    val PERSIAN_WEEK_DAYS = listOf(
        "شنبه",
        "یک‌شنبه",
        "دوشنبه",
        "سه‌شنبه",
        "چهارشنبه",
        "پنج‌شنبه",
        "جمعه"
    )

    val PERSIAN_WEEK_DAYS_SHORT = listOf(
        "ش", "ی", "د", "س", "چ", "پ", "ج"
    )

    val GREGORIAN_WEEK_DAYS = listOf(
        "Saturday", "Sunday", "Monday", "Tuesday",
        "Wednesday", "Thursday", "Friday"
    )

    /**
     * Converts western digits 0-9 into Persian digits ۰-۹
     */
    fun toPersianDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = java.lang.StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toPersianDigits(number: Int): String {
        return toPersianDigits(number.toString())
    }

    /**
     * Checks if a Persian year is leap year (366 days)
     */
    fun isPersianLeapYear(year: Int): Boolean {
        // Standard Persian calendar leap year 33-year cycle calculation
        val breaks = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
            1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
        )
        val n = breaks.size
        var jp = breaks[0]
        if (year < jp || year >= breaks[n - 1]) {
            val a = year - 474
            val b = (a % 2820 + 474) + 38
            return ((b * 682) % 2816) < 682
        }
        var jump = 0
        for (j in 1 until n) {
            val jm = breaks[j]
            jump = jm - jp
            if (year < jm) break
            jp = jm
        }
        var nYears = year - jp
        if (jump - nYears < 6) {
            nYears = nYears - jump + (jump + 4) / 33 * 33
        }
        var leap = ((nYears + 1) % 33) - 1
        if (leap == -1) leap = 4
        return (leap % 4 == 0) && (leap != 0)
    }

    /**
     * Returns total days in a Persian month (1-12)
     */
    fun getDaysInPersianMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            month == 12 -> if (isPersianLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    /**
     * Pure offline conversion of Gregorian (year, month 1-12, day 1-31)
     * into Persian Solar Hijri (year, month 1-12, day 1-31, dayOfWeek 1=Shanbeh..7=Jom'eh)
     */
    fun gregorianToPersian(gy: Int, gm: Int, gd: Int): PersianDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1

        var gDayNo = 365L * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
        for (i in 0 until gm2) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        var jd = 0
        for (i in 0 until 11) {
            if (jDayNo < jDaysInMonth[i]) {
                jm = i + 1
                jd = (jDayNo + 1).toInt()
                break
            }
            jDayNo -= jDaysInMonth[i]
        }
        if (jm == 0) {
            jm = 12
            jd = (jDayNo + 1).toInt()
        }

        // Calendar Day of Week: 1 = Sun, 2 = Mon, 3 = Tue, 4 = Wed, 5 = Thu, 6 = Fri, 7 = Sat
        val cal = Calendar.getInstance().apply {
            set(gy, gm - 1, gd)
        }
        val calDow = cal.get(Calendar.DAY_OF_WEEK)
        // Convert to Persian day of week:
        // 1=Shanbeh (Sat), 2=Yekshanbeh (Sun), 3=Doshanbeh (Mon), 4=Seshanbeh (Tue), 5=Chaharshanbeh (Wed), 6=Panjshanbeh (Thu), 7=Jom'eh (Fri)
        val pDow = when (calDow) {
            Calendar.SATURDAY -> 1
            Calendar.SUNDAY -> 2
            Calendar.MONDAY -> 3
            Calendar.TUESDAY -> 4
            Calendar.WEDNESDAY -> 5
            Calendar.THURSDAY -> 6
            Calendar.FRIDAY -> 7
            else -> 1
        }

        return PersianDate(jy.toInt(), jm, jd, pDow)
    }

    /**
     * Converts Persian date (jy, jm 1-12, jd 1-31) back to Gregorian (gy, gm 1-12, gd 1-31)
     */
    fun persianToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        val jy2 = jy - 979
        val jm2 = jm - 1
        val jd2 = jd - 1

        var jDayNo = 365L * jy2 + (jy2 / 33) * 8 + ((jy2 % 33 + 3) / 4)
        for (i in 0 until jm2) {
            jDayNo += jDaysInMonth[i]
        }
        jDayNo += jd2

        var gDayNo = jDayNo + 79

        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524

            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        val gDaysInMonth = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        var gd = 0
        for (i in 0 until 12) {
            if (gDayNo < gDaysInMonth[i]) {
                gm = i + 1
                gd = (gDayNo + 1).toInt()
                break
            }
            gDayNo -= gDaysInMonth[i]
        }

        return Triple(gy.toInt(), gm, gd)
    }

    /**
     * Gets full date info for today's current date
     */
    fun getTodayFullDateInfo(): FullDateInfo {
        val cal = Calendar.getInstance()
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        return getFullDateInfoForGregorian(gy, gm, gd)
    }

    /**
     * Generates FullDateInfo from Gregorian date components
     */
    fun getFullDateInfoForGregorian(gy: Int, gm: Int, gd: Int): FullDateInfo {
        val pDate = gregorianToPersian(gy, gm, gd)
        val isLeap = isPersianLeapYear(pDate.year)
        val daysInMonth = getDaysInPersianMonth(pDate.year, pDate.month)

        val pMonthName = PERSIAN_MONTH_NAMES[pDate.month - 1]
        val pDowName = PERSIAN_WEEK_DAYS[pDate.dayOfWeek - 1]
        val gMonthNameFa = GREGORIAN_MONTH_NAMES_PERSIAN[gm - 1]
        val gMonthNameEn = GREGORIAN_MONTH_NAMES_ENGLISH[gm - 1]
        val gDowName = GREGORIAN_WEEK_DAYS[pDate.dayOfWeek - 1]

        val pDayStr = if (pDate.day < 10) "0${pDate.day}" else "${pDate.day}"
        val pMonthStr = if (pDate.month < 10) "0${pDate.month}" else "${pDate.month}"
        val persianFormatted = toPersianDigits("${pDate.year}/$pMonthStr/$pDayStr")
        val persianFullTitle = "$pDowName ${toPersianDigits(pDate.day)} $pMonthName ${toPersianDigits(pDate.year)}"

        val gDayStr = if (gd < 10) "0$gd" else "$gd"
        val gMonthStr = if (gm < 10) "0$gm" else "$gm"
        val gregorianFormatted = "$gy-$gMonthStr-$gDayStr"

        return FullDateInfo(
            persianYear = pDate.year,
            persianMonth = pDate.month,
            persianDay = pDate.day,
            persianMonthName = pMonthName,
            persianDayOfWeekName = pDowName,
            persianFormatted = persianFormatted,
            persianFullTitle = persianFullTitle,
            gregorianYear = gy,
            gregorianMonth = gm,
            gregorianDay = gd,
            gregorianMonthNamePersian = gMonthNameFa,
            gregorianMonthNameEnglish = gMonthNameEn,
            gregorianDayOfWeekName = gDowName,
            gregorianFormatted = gregorianFormatted,
            isPersianLeapYear = isLeap,
            daysInPersianMonth = daysInMonth
        )
    }

    /**
     * Formats current time in 24-hour clock with Persian digits, e.g. "۱۴:۲۵:۰۸"
     */
    fun format24HourTimePersian(hours: Int, minutes: Int, seconds: Int): String {
        val h = if (hours < 10) "0$hours" else "$hours"
        val m = if (minutes < 10) "0$minutes" else "$minutes"
        val s = if (seconds < 10) "0$seconds" else "$seconds"
        return toPersianDigits("$h:$m:$s")
    }

    /**
     * Calculates delay in milliseconds until next midnight + 3 seconds.
     * Efficiently used by AlarmManager to wake up only once a day when the date changes.
     */
    fun getMillisUntilNextMidnight(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 3)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = midnight.timeInMillis - now.timeInMillis
        return if (diff > 0) diff else 24 * 3600 * 1000L
    }
}
