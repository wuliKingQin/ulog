package com.utopia.android.ulog.tools

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * des: 封装用于处理日期的工具类
 * author 秦王
 * time 2021/11/19 13:53
 */
internal class DateTool(
    // doc: 日期格式字符串
    private var dateFormatStr: String = DATE_ALL_FORMAT
) {

    companion object {
        // doc: 一天所需要的毫秒时间
        internal const val DAY_SIZE = 1000 * 3600 * 24
        const val DATE_ALL_FORMAT = "yyyy-MM-dd HH-mm-ss(SSS)"
    }

    private val mCalendar by lazy {
        Calendar.getInstance()
    }

    private var mDateFormat: SimpleDateFormat? = null

    /**
     * des: 将给的毫秒时间格式化成对于的时间字符串
     * time: 2021/11/19 14:24
     */
    fun format(timeMillis: Long): String {
        return getDateFormat().format(Date(timeMillis)) ?: ""
    }

    private fun getDateFormat(): SimpleDateFormat {
        if (mDateFormat == null) {
            mDateFormat = SimpleDateFormat(dateFormatStr, Locale.getDefault())
        }
        return mDateFormat!!
    }

    /**
     * des: 提供重新设置格式的方法
     * time: 2021/11/19 14:30
     */
    fun setDateFormat(dateFormatStr: String) {
        this.dateFormatStr = dateFormatStr
        mDateFormat = SimpleDateFormat(dateFormatStr, Locale.getDefault())
    }

    /**
     * des: 将字符串日期解析成Date对象
     * time: 2021/11/19 13:56
     */
    fun parseToDate(dateStr: String): Date? {
        return getDateFormat().parse(dateStr)
    }

    /**
     * des: 计算两个时间相隔几天
     * time: 2021/11/19 14:00
     */
    fun calculateFewDay(nowTimeMillis: Long, lastMillis: Long): Int {
        return ((nowTimeMillis - lastMillis) / DAY_SIZE).toInt()
    }

    /**
     * des: 使用之前的时间和现在的时间进行计算看他们相隔几天
     * time: 2021/11/19 14:06
     */
    fun calculateFewDay(lastDateStr: String?): Int {
        lastDateStr ?: return -1
        var date: Date? = null
        try {
            date = parseToDate(lastDateStr)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return if (date != null) {
            mCalendar.time = date
            calculateFewDay(System.currentTimeMillis(), mCalendar.timeInMillis)
        } else {
            -1
        }
    }

    /**
     * des: 计算两个日期是否是同一天
     * time: 2021/11/19 14:14
     */
    fun calculateTwoDateSameDay(lastDateStr: String?): Boolean {
        lastDateStr ?: return false
        var date: Date? = null
        try {
            date = parseToDate(lastDateStr)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return if (date != null) {
            mCalendar.time = date
            val lastYear = mCalendar.get(Calendar.YEAR)
            val lastMonth = mCalendar.get(Calendar.MONTH)
            val lastDay = mCalendar.get(Calendar.DAY_OF_MONTH)
            mCalendar.time = Date(System.currentTimeMillis())
            val nowYear = mCalendar.get(Calendar.YEAR)
            val nowMonth = mCalendar.get(Calendar.MONTH)
            val nowDay = mCalendar.get(Calendar.DAY_OF_MONTH)
            lastYear == nowYear && lastMonth == nowMonth && lastDay == nowDay
        } else {
            false
        }
    }

    /**
     * des: 比较两个日期的大小，如果异常返回-2
     * time: 2021/11/19 15:22
     */
    fun compareTowDate(dateStr1: String?, dateStr2: String?): Int {
        return when {
            dateStr2 == dateStr1 -> 0
            !dateStr1.isNullOrEmpty() && !dateStr2.isNullOrEmpty() -> {
                parseToDate(dateStr1)?.compareTo(parseToDate(dateStr2)) ?: -2
            }
            else -> -2
        }
    }

    /**
     * des: 获取当前日期字符串
     * time: 2021/11/24 16:43
     */
    fun getCurrentDate(dateFormatStr: String? = null): String {
        val dateFormatter = if (!dateFormatStr.isNullOrEmpty()) {
            SimpleDateFormat(dateFormatStr, Locale.getDefault())
        } else {
            getDateFormat()
        }
        return dateFormatter.format(Date(System.currentTimeMillis()))
    }
}