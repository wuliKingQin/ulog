package com.utopia.android.ulog.core

import com.utopia.android.ulog.core.message.MessageTask
import com.utopia.android.ulog.print.Printer
import java.util.*
import java.util.concurrent.BlockingQueue

/**
 * des: 抽象日志打印的工作类
 * author 秦王
 * time 2021/12/23 18:23
 */
interface Worker: Runnable, Executor {

    // doc: 用于判断是否还活着
    var isAlive: Boolean

    // doc: 打印器
    var printer: Printer

    // doc: 工作线程
    val thread: Thread

    // doc: 任务阻塞队列
    val taskQueue: BlockingQueue<MessageTask>

    // doc: 紧急任务，也就是当任务队列个数达到极限的时候，使用
    val hotTaskQueue: Queue<MessageTask>

    /**
     * des: 用来将任务添加到任务队列中，待执行
     * time: 2021/11/16 16:22
     */
    fun execute(task: MessageTask)
}