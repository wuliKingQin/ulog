package com.utopia.android.ulog.config.online

import android.content.Intent
import com.utopia.android.ulog.service.JobService

/**
 * des: 提供给接入层使用的配置更新抽象接口，可以继承该接口重写doWork方法，
 * 将新的配置信息返回，如果不需要进行更新，则返回null。实现该接口的配置信息，
 * 不会自动将配置信息进行存储，需要你自己实现，并通过getLastConfigModel方法去获取上一次的配置信息，
 * 这个方法会在初始化的时候去调用。
 * author 秦王
 * time 2021/12/28 9:41
 */
interface ConfigUpdater: JobService<ConfigModel> {
    /**
     * des: 配置更新地址
     * time: 2021/12/28 9:43
     */
    fun getConfigUrl(): String?

    /**
     * des: 配置的数据模型class
     * time: 2021/12/28 9:44
     */
    fun getConfigModel(): Class<*>

    override fun doWork(intent: Intent): ConfigModel? {
        return null
    }

    /**
     * des: 获取上一次的配置信息，该方法也需要自己实现
     * time: 2021/12/28 10:23
     */
    fun getLastConfigModel(): ConfigModel?

    /**
     * des: 在该方法中将数据模型转换成日志库需要的ConfigModel的子类型，
     * 假如你不重写doWork方法的话，必须要要实现该方法，
     * 是否更新配置，或者配置是否需要进行替换，都可以在该方法中进行实现。如果不需要更新配置返回null
     * time: 2021/12/28 10:05
     */
    fun onChangeConfigModel(configBo: Any?): ConfigModel?
}