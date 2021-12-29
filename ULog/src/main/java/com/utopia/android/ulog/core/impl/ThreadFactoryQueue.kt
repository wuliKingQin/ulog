package com.utopia.android.ulog.core.impl

import com.utopia.android.ulog.core.message.MessageTask
import java.util.concurrent.BlockingDeque
import java.util.concurrent.ThreadFactory

/**
 * des: 该接口用来配置打印器所在线程，以及存储缓存的阻塞队列
 * author 秦王
 * time 2021/12/29 14:24
 */
interface ThreadFactoryQueue: ThreadFactory {
    /**
     * des: 获取接入方的阻塞队列
     * time: 2021/12/29 14:26
     */
    fun getBlockingQueue(): BlockingDeque<MessageTask>
}