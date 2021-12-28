package com.utopia.android.ulog.print.filter

import com.utopia.android.ulog.core.message.UMessage

/**
 * des: 可以通过该接口实现一个过滤输出日志信息到控制台或者日志文件等，
 * 默认实现@DefaultFilter
 * author 秦王
 * time 2021/12/25 8:43
 */
interface Filter {
    /**
     * des: 是否过滤掉该条日志，返回false，表示不过滤，true表示过滤
     * time: 2021/11/16 17:03
     */
    fun isFilter(message: UMessage): Boolean
}