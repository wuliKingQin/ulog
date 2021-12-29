package com.utopia.android.ulog.print.interceptor

import com.utopia.android.ulog.core.message.UMessage

/**
 * des: 抽象出一个日志信息拦截器的接口，该接口在日志分发到各个打印器之前进行拦截
 * author 秦王
 * time 2021/12/25 8:51
 */
interface Interceptor {

    /**
     * des: 在该方法中拦截需要拦截的日志，将处理后的信息再次返回
     * time: 2021/12/29 14:01
     */
    fun onIntercept(message: UMessage): UMessage
}