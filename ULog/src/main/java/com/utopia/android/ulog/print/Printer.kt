package com.utopia.android.ulog.print

import com.utopia.android.ulog.core.message.UMessage
import com.utopia.android.ulog.print.filter.Filter

/**
 * des: 日志打印器接口类，抽象出共同的逻辑方法
 * author 秦王
 * time 2021/11/16 17:02
 */
interface Printer: Filter{
    /**
     * des: 打印器的名字，这里主要提供给线程作为线程名
     * time: 2021/11/16 17:03
     */
    fun name(): String = javaClass.simpleName

    override fun isFilter(message: UMessage) = false

    /**
     * des: 真正开始执行打印日志的逻辑
     * time: 2021/11/16 17:04
     */
    fun print(message: UMessage)
}