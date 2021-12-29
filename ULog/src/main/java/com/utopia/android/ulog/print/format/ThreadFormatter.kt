package com.utopia.android.ulog.print.format

/**
 * des: 线程格式化器
 * author 秦王
 * time 2021/12/29 17:09
 */
class ThreadFormatter: Formatter<Thread?> {

    override fun format(data: Thread?): String {
        return if (data != null) {
            " Thread: ${data.name} id: ${data.id} isAlive: ${data.isAlive} isDaemon: ${data.isDaemon}"
        } else {
            ""
        }
    }
}