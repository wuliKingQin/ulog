package com.utopia.android.ulog.print.format

/**
 * des: 用来实现日志信息的格式化接口
 * author 秦王
 * time 2021/12/25 10:06
 */
interface Formatter<T> {
    /**
     * des: 格式化方法
     * time: 2021/11/18 9:16
     */
    fun format(data: T): String
}