package com.utopia.android.ulog.core

import com.utopia.android.ulog.core.message.MessageTask
import com.utopia.android.ulog.core.message.UMessage
import java.lang.Exception


/**
 * des: 该类主要用于封装消息分发到各个工作队列中
 * author 秦王
 * time 2021/12/23 18:19
 */
class Dispatcher(
    private var workerPool: WorkerPool
) {

    // doc: 防止重复创建需要移除的worker
    private val mRemoveWorksKey by lazy {
        ArrayList<Class<*>>()
    }

    // doc: 正在开始执行的执行缓存
    private val mRealExecWorkerMap by lazy {
        HashMap<Class<*>, Worker>()
    }

    /**
     * des: 将消息进行分发到各个工作器
     * time: 2021/12/23 19:36
     */
    fun dispense(message: UMessage) {
        synchronized(this) {
            callAndClearUpWorkers(message)
        }
    }

    /**
     * des: 执行方法并整理工作器
     * time: 2021/12/23 19:35
     */
    private fun callAndClearUpWorkers(message: UMessage) {
        try {
            if (workerPool.readyWorkers.isNotEmpty()) {
                val workers = ArrayList<Worker>(workerPool.readyWorkers)
                workerPool.readyWorkers.clear()
                for (worker in workers) {
                    if (worker.isAlive) {
                        if (!worker.thread.isInterrupted) {
                            try {
                                worker.thread.start()
                                workerPool.runningWorkers[worker.printer.javaClass] = worker
                            } catch (e: Exception) {
                                workerPool.addWorker(worker.printer::class.java, message)
                            }
                        } else {
                            workerPool.addWorker(worker.printer::class.java, message)
                        }
                    }
                }
            }
            if (workerPool.runningWorkers.isNotEmpty()) {
                realExecMsgDispense(message)
            }
        } finally {
            clearUpWorkers(message)
        }
    }

    /**
     * des: 整理并重新创建已经死掉的worker
     * time: 2021/12/23 19:35
     */
    private fun clearUpWorkers(message: UMessage) {
        try {
            var remTaskList: ArrayList<MessageTask>? = null
            for (key in mRemoveWorksKey) {
                remTaskList?.clear()
                val oldWorker = workerPool.runningWorkers[key]
                workerPool.runningWorkers.remove(key)
                if (!oldWorker?.taskQueue.isNullOrEmpty()) {
                    if (remTaskList == null) {
                        remTaskList = ArrayList()
                    }
                    remTaskList.addAll(oldWorker?.taskQueue!!)
                    remTaskList.add(MessageTask.obtain().apply {
                        makeUse()
                        printer = oldWorker.printer
                        this.message = message
                    })
                    workerPool.addWorker(key, tasks = *remTaskList.toTypedArray())
                } else {
                    workerPool.addWorker(key, firstMessage = message)
                }
            }
        } finally {
            mRemoveWorksKey.clear()
            mRealExecWorkerMap.clear()
        }
    }

    private fun realExecMsgDispense(message: UMessage) {
        mRealExecWorkerMap.putAll(workerPool.runningWorkers)
        var worker: Worker? = null
        val workerKeys = mRealExecWorkerMap.keys
        message.makeUse(if (workerKeys.isEmpty()) 0 else workerKeys.size - 1)
        for (workerKey in workerKeys) {
            worker = mRealExecWorkerMap[workerKey]
            if (worker != null) {
                if (worker.isAlive && !worker.thread.isInterrupted) {
                    if (!worker.printer.isFilter(message)) {
                        worker.execute(message)
                    } else {
                        // doc: 需要回收处理，减少new的次数
                        message.recycle()
                    }
                } else {
                    try {
                        if (!worker.thread.isInterrupted) {
                            worker.isAlive = false
                            worker.thread.interrupt()
                        }
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }
                    mRemoveWorksKey.add(workerKey)
                }
            } else {
                mRemoveWorksKey.add(workerKey)
            }
        }
    }
}