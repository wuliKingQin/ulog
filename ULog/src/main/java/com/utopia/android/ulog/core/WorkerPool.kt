package com.utopia.android.ulog.core

import com.utopia.android.ulog.core.message.MessageTask
import com.utopia.android.ulog.core.message.UMessage
import com.utopia.android.ulog.print.Printer
import java.lang.Exception

/**
 * des: 工作器池子
 * author 秦王
 * time 2021/12/23 19:57
 */
class WorkerPool(
    // doc: 工作器创建器
    private val workerCreator: (Printer, UMessage?)-> Worker
) {

    companion object {
        private const val DEFAULT_WORKER_SIZE = 8
    }

    // doc: 准好的好的工作列表，用于临时进行存储
    internal val readyWorkers by lazy {
        ArrayList<Worker>(DEFAULT_WORKER_SIZE)
    }

    // doc: 执行中的工作列表
    internal val runningWorkers by lazy {
        HashMap<Class<*>, Worker>(DEFAULT_WORKER_SIZE)
    }

    /**
     * des: 添加工作器
     * time: 2021/12/23 18:30
     */
    private fun addWorker(worker: Worker) {
        val isExist = readyWorkers.any {
            it.printer::class.java == worker.printer::class.java || it == worker
        }
        if (!isExist) {
            readyWorkers.add(worker)
        }
    }

    /**
     * des: 添加一个打印器到工作器中，如果之前有相同的打印器，
     * 则移除之前的，重新创建一个新的并把他之前的任务拷贝过来
     * time: 2021/11/26 11:37
     */
    fun addWorker(printer: Printer) {
        val printerCls = printer.javaClass
        var remTaskList: ArrayList<MessageTask>? = null
        if (runningWorkers.containsKey(printerCls)) {
            val oldWork = runningWorkers[printerCls]
            if (printer == oldWork?.printer && oldWork.isAlive) {
                return
            } else {
                runningWorkers.remove(printerCls)
                try {
                    oldWork?.isAlive = false
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
                if (!oldWork?.taskQueue.isNullOrEmpty()) {
                    remTaskList = ArrayList(oldWork!!.taskQueue)
                    oldWork.taskQueue.clear()
                }
            }
        }
        val worker = workerCreator(printer, null).apply {
            if (remTaskList?.isNotEmpty() == true) {
                for (task in remTaskList) {
                    task.printer = printer
                    execute(task)
                }
            }
        }
        addWorker(worker)
    }

    /**
     * des: 在内部调用的添加工作器方法，
     * 该方法可以将未执行的任务在工作器创建好了以后，继续添加执行
     * time: 2021/12/23 19:55
     */
    internal fun addWorker(
        clsPrinter: Class<*>,
        firstMessage: UMessage? = null,
        vararg tasks: MessageTask
    ) {
        if (!runningWorkers.containsKey(clsPrinter)) {
            val printer = try {
                clsPrinter.newInstance() as? Printer
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            if (printer != null) {
                val worker = workerCreator(printer, firstMessage).apply {
                    for (task in tasks) {
                        task.printer = printer
                        execute(task)
                    }
                }
                addWorker(worker)
            }
        }
    }
}