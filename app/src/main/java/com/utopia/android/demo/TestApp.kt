package com.utopia.android.demo

import android.app.Application
import com.utopia.android.ulog.ULog
import com.utopia.android.ulog.config.UConfig
import com.utopia.android.ulog.config.online.ConfigModel
import com.utopia.android.ulog.config.online.ConfigUpdater
import com.utopia.android.ulog.core.impl.ThreadFactoryQueue
import com.utopia.android.ulog.core.message.MessageTask
import com.utopia.android.ulog.print.file.FilePrinter
import com.utopia.android.ulog.print.file.upload.AbstractFileUploader
import com.utopia.android.ulog.print.file.upload.Uploader
import com.utopia.android.ulog.print.file.writer.DefaultFileWriter
import com.utopia.android.ulog.print.file.writer.Writer
import com.utopia.android.ulog.print.logcat.AndroidPrinter
import java.io.File
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque

class TestApp: Application(){

    override fun onCreate() {
        super.onCreate()
        val fileWriter = DefaultFileWriter(object : Writer.FileReady {
            override fun onReady(writer: Writer) {
                writer.writeToFileHead("=========file head=========")
            }
        })
        val filePrinter = FilePrinter.Builder()
            .setFileWriter(fileWriter)
            .build()
        val config = UConfig.Builder()
            .setDebug(BuildConfig.DEBUG)
            .setLogFilePrefixName(packageName)
            .setCacheLogDir(File(externalCacheDir?.absolutePath, "log").path)
            .addPrinter(filePrinter)
            .addPrinters(AndroidPrinter::class.java)
            .setThreadFactory(object : ThreadFactoryQueue {
                override fun getBlockingQueue(): BlockingDeque<MessageTask> {
                    return LinkedBlockingDeque<MessageTask>(1000)
                }

                override fun newThread(runnable: Runnable?): Thread {
                    return Thread(runnable)
                }
            })
            .setConfigUpdater(ConfigUpdaterImpl())
            .setFileUploader(UploaderImpl(object :Uploader.UploadInfo{
                override fun getUploadUrl(): String {
                    return ""
                }
                override fun getUniqueIdentity(): String {
                    return ""
                }
                override fun getGrayIdentity(): String {
                    return ""
                }
            }))
            .build()
        ULog.init(this, config)
    }

    class ConfigUpdaterImpl: ConfigUpdater {
        override fun getConfigUrl(): String {
            return ""
        }

        override fun getConfigModel(): Class<*> {
            return ConfigBo::class.java
        }

        override fun getLastConfigModel(): ConfigModel? {
            return null
        }

        override fun onChangeConfigModel(configBo: Any?): ConfigModel? {
            if (configBo is ConfigBo) {
                return configBo.data
            }
            return null
        }

        data class ConfigBo(
            var code: Int = 0,
            var message: String? = null,
            var data:Data? = null
        ) {
            data class Data(
                private var maxLogFileSize: Int = 0,
                private var maxCacheDirSize: Int = 0,
                private var remainingDiskSize: Int = 0,
                private var logValidTime: Int = 0
            ): ConfigModel {

                override fun getAutoUploadWhiteList(): List<String>? {
                    return null
                }

                override fun getMaxLogFileSize(): Int {
                    return maxLogFileSize
                }

                override fun getMaxCacheDirSize(): Int {
                    return maxCacheDirSize
                }

                override fun getRemainingDiskSize(): Int {
                    return remainingDiskSize
                }

                override fun getLogValidTime(): Int {
                    return logValidTime
                }
            }
        }
    }

    class UploaderImpl(
        uploadInfo: Uploader.UploadInfo
    ) : AbstractFileUploader(uploadInfo) {

        override fun startUpload(logZipFile: File, resultCallback: Uploader.ResultCallback) {
            Thread.sleep(10000)
            resultCallback.onUploadSuccess()
        }
    }
}