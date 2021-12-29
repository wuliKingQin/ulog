package com.utopia.android.ulog.core.message

import android.util.Log
import androidx.annotation.IntDef
import com.utopia.android.ulog.config.UConfig

/**
 * des: 用于记录日志的信息包裹类
 * time: 2021/12/10 19:01
 */
data class UMessage(
    // doc: 日志级别，默认是DEBUG类型
    var level: Int = Log.DEBUG,
    // doc: 日志tag名
    var tag: String? = null,
    // doc: 日志内容
    var content: Any? = null,
    // doc: 日志打印的时间
    var time: Long = 0,
    // doc: 消息类型
    var type: Int = LOG,
    // doc: 是否写入到日志文件
    var isWriteToFile: Boolean = true,
    // doc: 获取配置信息
    var config: UConfig? = null,
    // doc: 产生日志的线程
    var thread: Thread? = null
): Usable() {

    var next: UMessage? = null

    // doc: 用于可变参数
    internal var otherArgs: Map<String, Any>? = null

    companion object {
        const val LOG = 1
        const val EVENT = 2
        const val EVENT_FLUSH = 3
        private val mSyncObj = Any()
        // 池子最大值为50个
        private const val POOL_MAX_SIZE = 50
        private var mPoolSize = 0
        private var mRecordPool: UMessage? = null

        // 获取实例
        fun obtain(): UMessage {
            synchronized(mSyncObj) {
                if (mRecordPool != null) {
                    val record = mRecordPool
                    mRecordPool = record!!.next
                    record.next = null
                    mPoolSize --
                    return record
                }
            }
            return UMessage()
        }
    }

    /**
     * des: 通过该方法获取传去的其他参数，用于扩展使用
     * time: 2021/12/26 11:17
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgs(key: String, default: T? = null): T? {
        return (otherArgs?.get(key) as? T) ?: default
    }

    /**
     * des: 用于销毁消息
     * time: 2021/12/23 19:00
     */
    fun recycle() {
        checkUse { recycleUnchecked() }
    }

    /**
     * des: 回收使用完毕的消息
     * time: 2021/12/23 19:00
     */
    private fun recycleUnchecked() {
        tag = null
        time = 0
        content = null
        level = Log.DEBUG
        type = LOG
        isWriteToFile = true
        config = null
        thread = null
        synchronized(mSyncObj) {
            if (mPoolSize < POOL_MAX_SIZE) {
                next = mRecordPool
                mRecordPool = this
                mPoolSize ++
            }
        }
    }

    /**
     * des: 消息类型，分为日志和事件，事件用于内部通信
     * author 秦王
     * time 2021/12/23 19:02
     */
    @IntDef(LOG, EVENT, EVENT_FLUSH)
    annotation class MessageType
}
