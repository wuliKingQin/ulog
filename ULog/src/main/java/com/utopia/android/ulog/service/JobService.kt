package com.utopia.android.ulog.service

import android.content.Context
import android.content.Intent
import android.util.SparseArray
import androidx.core.app.JobIntentService

/**
 * des: 定义一个Job服务，用来给上传以及更新配置使用
 * author 秦王
 * time 2021/12/27 19:16
 */
interface JobService<T> {

    companion object {

        // doc: 服务的唯一的Id
        const val JOB_ID = 0x10086
        // doc: 任务类型
        const val JOB_TYPE = "jobType"
        // doc: 上传服务
        internal const val UPLOAD_JOB = 1
        // doc: 配置更新服务
        internal const val CONFIG_UPDATE_JOB = 2
        // doc: 用于缓存服务
        private val mCacheServicesMap by lazy {
            SparseArray<JobService<*>>()
        }

        /**
         * des: 添加服务
         * time: 2021/12/28 10:41
         */
        fun putService(type: Int, service: JobService<*>) {
            mCacheServicesMap.put(type, service)
        }

        /**
         * des: 获取服务
         * time: 2021/12/28 10:42
         */
        fun getService(type: Int): JobService<*>? {
            return mCacheServicesMap[type]
        }

        /**
         * des: 将任务进行入队
         * time: 2021/12/27 19:32
         */
        inline fun enqueueWork(context: Context, jobType: Int, intentAction: Intent.() -> Unit) {
            getService(jobType) ?: return
            JobIntentService.enqueueWork(
                context,
                LoggerJobService::class.java,
                JOB_ID,
                Intent().apply {
                    putExtra(JOB_TYPE, jobType)
                    intentAction()
                }
            )
        }
    }

    /**
     * des: 在该方法中进行任务的逻辑, 返回值true表示需要拦截，有上一层进行处理
     * time: 2021/12/27 19:17
     */
    fun doWork(intent: Intent): T?
}