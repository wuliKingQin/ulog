package com.utopia.android.ulog.config.online

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.utopia.android.ulog.extend.request
import com.utopia.android.ulog.service.JobService

/**
 * des: 用于在线配置的拉取以及更新服务
 * author 秦王
 * time 2021/12/27 19:25
 */
internal class ConfigJobService(
    // doc: 用户提供的配置更新实现类
    private var configUpdater: ConfigUpdater
) : JobService<ConfigModel> {

    companion object {

        /**
         * des: 添加接入层实现的配置服务
         * time: 2021/12/28 10:49
         */
        fun addService(configUpdater: ConfigUpdater?) {
            if (configUpdater != null) {
                JobService.putService(
                    JobService.CONFIG_UPDATE_JOB,
                    ConfigJobService(configUpdater)
                )
            }
        }

        /**
         * des: 提供更新配置的入口方法
         * time: 2021/12/28 10:33
         */
        fun startUpdate(context: Context) {
            JobService.enqueueWork(context, JobService.CONFIG_UPDATE_JOB) {
            }
        }
    }

    override fun doWork(intent: Intent): ConfigModel? {
        var configModel = configUpdater.doWork(intent)
        if (configModel != null) {
            return configModel
        }
        val configUrl = configUpdater.getConfigUrl() ?: return null
        val configModelCls = configUpdater.getConfigModel() ?: return null
        configUrl.request { success, response ->
            if (success && !response.isNullOrEmpty()) {
                val configBo = Gson().fromJson(response, configModelCls)
                configModel = configUpdater.onChangeConfigModel(configBo)
            }
        }
        return configModel
    }
}