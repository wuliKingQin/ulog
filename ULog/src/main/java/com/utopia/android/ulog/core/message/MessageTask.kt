package com.utopia.android.ulog.core.message

import com.utopia.android.ulog.print.Printer


/**
 * des: 把消息包裹成一个消息打印的任务
 * time: 2021/12/23 18:50
 */
class MessageTask(
    var printer: Printer? = null,
    var message: UMessage? = null
): Usable(), Runnable {

    companion object {
        private val mSyncObj = Any()
        // 池子最大大小
        private const val POOL_MAX_SIZE = 50
        private var mPoolSize = 0
        private var mRecordPool: MessageTask? = null
        // 获取实例
        fun obtain(): MessageTask {
            synchronized(mSyncObj) {
                if (mRecordPool != null) {
                    val record = mRecordPool
                    mRecordPool = record!!.next
                    record.next = null
                    mPoolSize--
                    return record
                }
            }
            return MessageTask()
        }
    }

    var next: MessageTask? = null

    /**
     * des: 判断是否达到回收任务
     * time: 2021/12/23 19:06
     */
    fun recycle() {
        checkUse { recycleUnchecked() }
    }

    /**
     * des: 回收消息该任务
     * time: 2021/12/23 19:06
     */
    private fun recycleUnchecked() {
        printer = null
        message = null
        synchronized(mSyncObj) {
            if (mPoolSize < POOL_MAX_SIZE) {
                next = mRecordPool
                mRecordPool = this
                mPoolSize++
            }
        }
    }

    override fun run() {
        message?.apply {
            try {
                printer?.print(this)
            } finally {
                recycle()
            }
        }
    }
}