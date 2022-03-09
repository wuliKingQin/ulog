package com.utopia.android.ulog.core.impl

import com.utopia.android.ulog.core.Dispatcher
import com.utopia.android.ulog.core.Executor
import com.utopia.android.ulog.core.Worker
import com.utopia.android.ulog.core.WorkerPool
import com.utopia.android.ulog.core.message.MessageTask
import com.utopia.android.ulog.core.message.UMessage
import com.utopia.android.ulog.print.Printer
import java.lang.Exception
import java.util.*
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque

/**
 * des: 该类封装为打印器的执行，用来添加打印器，执行消息等
 * author 秦王
 * time 2021/12/13 19:33
 */
class PrintExecutor: Executor {

    // doc: 用于创建线程的线程工厂
    internal var threadFactoryQueue: ThreadFactoryQueue? = null
    // doc: 工作池，用来添加打印器
    private var mWorkerPool: WorkerPool? = null
    // doc: 用于日志的分发工作
    private var mDispatcher: Dispatcher? = null

    override fun execute(message: UMessage) {
        checkDispatcher()
        mDispatcher?.dispense(message)
    }

    private fun checkDispatcher() {
        if (mWorkerPool == null) {
            mWorkerPool = WorkerPool { printer, uMessage ->
                createWorker(printer, uMessage)
            }
        }
        mWorkerPool?.apply {
            if (mDispatcher == null) {
                mDispatcher = Dispatcher(this)
            }
        }
    }

    /**
     * des: 获取工作池子
     * time: 2021/12/26 10:56
     */
    fun getWorkerPool(): WorkerPool {
        checkDispatcher()
        return mWorkerPool!!
    }

    private fun createWorker(printer: Printer, uMessage: UMessage?): Worker {
        if (threadFactoryQueue !is ThreadFactoryQueueWrapper) {
            threadFactoryQueue = ThreadFactoryQueueWrapper(threadFactoryQueue, printer.name())
        }
        return WorkerImpl(threadFactoryQueue!!, printer, uMessage)
    }

    /**
     * des: 打印封装的执行日志的任务工作类
     * author 秦王
     * time 2021/11/16 16:50
     */
    inner class WorkerImpl(
        private var threadFactoryQueue: ThreadFactoryQueue,
        override var printer: Printer,
        message: UMessage? = null
    ) : Worker {

        @Volatile
        override var isAlive: Boolean = true

        private var firstTask: MessageTask? = null

        override val taskQueue: BlockingDeque<MessageTask> by lazy {
            threadFactoryQueue.getBlockingQueue()
        }

        override val hotTaskQueue: Queue<MessageTask> by lazy {
            LinkedList<MessageTask>()
        }

        override val thread: Thread by lazy {
            threadFactoryQueue.newThread(this)
        }

        init {
            if (message != null) {
                firstTask = createMessageTask(message)
            }
        }

        override fun execute(task: MessageTask) {
            taskQueue.offer(task)
        }

        override fun execute(message: UMessage) {
            val recordTask = createMessageTask(message)
            if (message.type == UMessage.LOG) {
                taskQueue.offer(recordTask)
            } else {
                val isOfferSuccess = taskQueue.offerFirst(recordTask)
                if (!isOfferSuccess) {
                    synchronized(printer) {
                        hotTaskQueue.offer(recordTask)
                    }
                }
            }
        }

        /**
         * des: 创建Record任务
         * time: 2021/11/17 11:32
         */
        private fun createMessageTask(message: UMessage?): MessageTask {
            return MessageTask.obtain().apply {
                makeUse()
                this.printer = this@WorkerImpl.printer
                this.message = message
            }
        }

        override fun run() {
            doWork()
        }

        /**
         * des: 该方法真正开始执行任务
         * time: 2021/11/26 17:29
         */
        private fun doWork() {
            var task = firstTask
            try {
                while (isAlive && !thread.isInterrupted) {
                    try {
                        task?.run()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        task?.recycle()
                    }
                    if (!isAlive) {
                        break
                    }
                    try {
                        if (hotTaskQueue.isNotEmpty()) {
                            // doc: 处理紧急任务
                            task = hotTaskQueue.poll()
                            continue
                        }
                    } catch (e: Exception) {
                    }
                    try {
                        task = taskQueue.take()
                    } catch (e: InterruptedException) {
                        isAlive = false
                    }
                }
                checkTaskLeaksAndClear()
            } finally {
                task = null
            }
        }

        /**
         * des: 线程结束，检查是否还有任务在队列里面，
         * 如果还有，则先对象任务进行回收，然后在清除
         * time: 2021/11/26 17:31
         */
        private fun checkTaskLeaksAndClear(){
            if (taskQueue.isNotEmpty()) {
                for (t in taskQueue) {
                    t.message?.recycle()
                    t?.recycle()
                }
                taskQueue.clear()
            }
        }
    }

    /**
     * des: 用于创建线程的工程包装类，可以接受接入方的线程创建工厂
     * author 秦王
     * time 2021/12/27 9:49
     */
    class ThreadFactoryQueueWrapper(
        private var parent: ThreadFactoryQueue?,
        private var threadName: String
        ): ThreadFactoryQueue {

        companion object {
            // doc: 为了防止一下子存储阻塞队列的日志过多造成的OOM，所以设置了最大缓存个数1000
            private const val MAX_TASK_COUNT = 1000
        }

        override fun getBlockingQueue(): BlockingDeque<MessageTask> {
            return parent?.getBlockingQueue() ?: LinkedBlockingDeque<MessageTask>(MAX_TASK_COUNT)
        }

        override fun newThread(runnale: Runnable?): Thread {
            return (parent?.newThread(runnale) ?: Thread(runnale)).apply {
                val lastName = if (name.isNullOrEmpty()) {
                    ""
                } else {
                    "${name}_"
                }
                name = "$lastName${threadName}"
                isDaemon = false
            }
        }
    }
}

