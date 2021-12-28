package com.utopia.android.ulog.core

import com.utopia.android.ulog.core.message.UMessage

/**
 * des: 日志打印执行接口
 * author 秦王
 * time 2021/11/16 15:40
 */
interface Executor {
    /**
     * des: 执行方法，要用它来提交对日志的打印
     * time: 2021/11/16 15:41
     */
    fun execute(message: UMessage)
}