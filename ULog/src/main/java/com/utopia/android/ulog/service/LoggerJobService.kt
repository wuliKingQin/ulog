package com.utopia.android.ulog.service

import android.content.Intent
import androidx.core.app.JobIntentService
import com.utopia.android.ulog.ULog
import com.utopia.android.ulog.config.online.ConfigModel

/**
 * des: 日志任务服务，用于日志的上传以及配置更新等
 * author 秦王
 * time 2021/12/27 19:13
 */
internal class LoggerJobService: JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        val serviceType = intent.getIntExtra(JobService.JOB_TYPE, -1)
        val result = JobService.getService(serviceType)?.doWork(intent)
        if (result is ConfigModel) {
            // doc: 更新在线配置操作
            ULog.setOnlineConfig(result)
        }
    }
}