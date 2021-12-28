package com.utopia.android.ulog

import android.content.Context
import android.util.Log
import com.utopia.android.ulog.config.UConfig
import com.utopia.android.ulog.config.online.ConfigModel
import com.utopia.android.ulog.config.online.ConfigJobService
import com.utopia.android.ulog.print.file.upload.Uploader

/**
 * des: 日志工具类的入口类
 * author 秦王
 * time 2021/12/10 18:39
 */
object ULog {

    // doc: 实例化日志输出的类
    private val mLogger by lazy {
        ULogger()
    }

    /**
     * des: 出现化日志的一些配置工作，该方法必须在Application的onCreate方法中进行调用
     * time: 2021/12/10 18:46
     */
    @JvmStatic
    fun init(context: Context, config: UConfig) {
        mLogger.init(context, config)
    }

    /**
     * des: 打印类型为Log.DEBUG的日志方法,
     * 调用该方法会同时将日志写入到Logcat以及日志文件里面
     * time: 2021/12/10 18:52
     */
    @JvmStatic
    @JvmOverloads
    fun d(tag: String, message: Any?, otherArgs: Map<String, Any>? = null) {
        mLogger.println(Log.DEBUG, tag, message, otherArgs)
    }

    /**
     * des: 打印类型为Log.INFO的日志方法,
     * 调用该方法会同时将日志写入到Logcat以及日志文件里面
     * time: 2021/12/10 18:52
     */
    @JvmStatic
    @JvmOverloads
    fun i(tag: String, message: Any?, otherArgs: Map<String, Any>? = null) {
        mLogger.println(Log.INFO, tag, message, otherArgs)
    }

    /**
     * des: 打印类型为Log.VERBOSE的日志方法,
     * 调用该方法会同时将日志写入到Logcat以及日志文件里面
     * time: 2021/12/10 18:52
     */
    @JvmStatic
    @JvmOverloads
    fun v(tag: String, message: Any?, otherArgs: Map<String, Any>? = null) {
        mLogger.println(Log.VERBOSE, tag, message, otherArgs)
    }

    /**
     * des: 打印类型为Log.ERROR的日志方法,
     * 调用该方法会同时将日志写入到Logcat以及日志文件里面
     * time: 2021/12/10 18:50
     */
    @JvmStatic
    @JvmOverloads
    fun e(tag: String, message: Any?, otherArgs: Map<String, Any>? = null) {
        mLogger.println(Log.ERROR, tag, message, otherArgs)
    }

    /**
     * des: 该方法只在debug环境下打印到logcat里面的，不写入日志文件，默认log类型是Debug类型
     * time: 2021/12/10 18:48
     */
    @JvmStatic
    fun toLogcat(tag: String, message: Any?) {
        if (isDebug()) {
            mLogger.printlnToAndroid(Log.DEBUG, tag, message)
        }
    }

    /**
     * des: 该方法只在debug环境下打印到logcat里面的，不写入日志文件，默认log类型是Debug类型
     * 该方法的tag是自动获取，格式：类名_方法名
     * time: 2021/12/10 18:48
     */
    @JvmStatic
    @JvmOverloads
    fun trace(message: Any?, deep: Int = 1) {
        if (!isDebug()) return
        val exception = Throwable()
        val className = exception.stackTrace[deep].className
        val tag = "${className.split(".").lastOrNull()}_${
            exception.stackTrace[deep].methodName
        }"
        toLogcat(tag, message)
    }

    /**
     * des: post一条异步消息到日志框架，这条消息可以优先进行处理
     * type只有UMessage.EVENT_FLUSH、UMessage.EVENT或者其他自己定义的类型
     * time: 2021/12/26 13:26
     */
    @JvmStatic
    fun postAsync(message: Any, type: Int) {
        mLogger.postAsync(message, type)
    }

    /**
     * des: 将日志上传到服务器
     * time: 2021/12/27 12:36
     */
    @JvmOverloads
    @JvmStatic
    fun uploadToServer(
        context: Context,
        uploadCondition: Uploader.Condition? = null,
        isSilentlyUpload: Boolean = true,
        isCheckUseInfo: Boolean = false
    ) {
        mLogger.uploadToServer(context, uploadCondition, isSilentlyUpload, isCheckUseInfo)
    }

    /**
     * des: 用于更新线上的配置信息, 调用该方法必须要要实现ConfigUpdater接口，否则调用无效
     * time: 2021/12/28 10:59
     */
    fun startConfigUpdate(context: Context) {
        ConfigJobService.startUpdate(context)
    }

    /**
     * des: 设置在线配置实例
     * time: 2021/12/27 19:52
     */
    internal fun setOnlineConfig(onlineConfig: ConfigModel?) {
        mLogger.setOnlineConfig(onlineConfig)
    }

    /**
     * des: 获取是否处于debug模式
     * time: 2021/12/27 19:52
     */
    internal fun isDebug(): Boolean {
        return mLogger.config?.isDebug ?: false
    }
}