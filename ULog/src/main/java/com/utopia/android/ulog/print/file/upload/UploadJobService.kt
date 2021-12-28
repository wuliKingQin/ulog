package com.utopia.android.ulog.print.file.upload

import android.content.Context
import android.content.Intent
import com.utopia.android.ulog.ULog
import com.utopia.android.ulog.core.message.UMessage
import com.utopia.android.ulog.extend.splitFileNameInDate
import com.utopia.android.ulog.service.JobService
import com.utopia.android.ulog.tools.DateTool
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * des: 定义一个日志上传服务，用于日志的上传操作
 * author 秦王
 * time 2021/12/27 11:34
 */
internal class UploadJobService(
    private var uploader: Uploader
) : JobService<Unit> {

    companion object {

        // doc: 用于获取日志的缓存目录
        private const val CACHE_LOG_DIR = "cacheLogDir"

        // doc: 默认后缀名
        private const val ZIP_SUFFIX_NAME = ".zip"

        // doc: 用于标识是否正在上传日志，防止重复上传
        @Volatile
        var isUploading = false

        /**
         * des: 添加接入层实现的配置服务
         * time: 2021/12/28 10:49
         */
        fun addService(uploader: Uploader?) {
            if (uploader != null) {
                JobService.putService(
                    JobService.UPLOAD_JOB,
                    UploadJobService(uploader)
                )
            }
        }

        /**
         * des: 开始上传日志文件
         * time: 2021/12/27 11:46
         */
        fun startUpload(context: Context, cacheLogDir: String?) {
            JobService.enqueueWork(context, JobService.UPLOAD_JOB) {
                putExtra(CACHE_LOG_DIR, cacheLogDir)
            }
        }
    }

    override fun doWork(intent: Intent) {
        try {
            val cacheDir = intent.getStringExtra(CACHE_LOG_DIR)
            if (!cacheDir.isNullOrEmpty()) {
                isUploading = true
                executeFlush()
                val cacheDirFile = File(cacheDir)
                val logZipFile = logFilesZip(cacheDirFile, uploader)
                if (logZipFile != null) {
                    try {
                        val resultCallback = ResultCallbackImpl(
                            cacheDirFile, logZipFile, uploader
                        )
                        uploader.startUpload(logZipFile, resultCallback)
                    } catch (e: Exception) {
                        isUploading = false
                        e.printStackTrace()
                    }
                } else {
                    isUploading = false
                }
            }
        } catch (e: Exception) {
            isUploading = false
            e.printStackTrace()
        }
    }

    /**
     * des: 上传日志之前先将日志信息从映射区域flush一遍到日志文件里
     * time: 2021/12/27 11:24
     */
    private fun executeFlush() {
        ULog.postAsync("uploader send flush message", UMessage.EVENT_FLUSH)
    }

    /**
     * des:  将log文件打包成zip文件
     * time: 2021/11/24 13:58
     */
    private fun logFilesZip(cacheDirFile: File, fileUploader: Uploader): File? {
        return try {
            doZip(cacheDirFile, fileUploader)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * des: 多文件压缩的工作
     * time: 2021/12/27 11:27
     */
    private fun doZip(cacheFileDir: File, fileUploader: Uploader): File? {
        val dateTool = DateTool()
        return if (cacheFileDir.exists() && cacheFileDir.isDirectory) {
            var zipFileName = fileUploader.modifyUploadFileName()
            val suffixName = zipFileName?.split(".")?.lastOrNull() ?: ZIP_SUFFIX_NAME
            val uploadFiles = cacheFileDir.listFiles { file ->
                // doc: 防止有时上传失败重复上传之前的压缩包
                if (file.isFile && !file.name.endsWith(suffixName)) {
                    // doc: 计算是否当天的日志
                    val fileDate = file.splitFileNameInDate()
                    val result = fileUploader.shouldAddToZipFile(file)
                    if (result == -1) {
                        dateTool.calculateTwoDateSameDay(fileDate)
                    } else {
                        result == 1
                    }
                } else {
                    false
                }
            }
            if (!uploadFiles.isNullOrEmpty()) {
                zipFileName = if (zipFileName.isNullOrEmpty()) {
                    "logs_${dateTool.getCurrentDate()}${ZIP_SUFFIX_NAME}"
                } else if (!zipFileName.endsWith(ZIP_SUFFIX_NAME)) {
                    "${zipFileName}${ZIP_SUFFIX_NAME}"
                } else {
                    zipFileName
                }
                val zipFile = File(cacheFileDir, zipFileName)
                ZipOutputStream(FileOutputStream(zipFile)).use { zipOutput ->
                    for (file in uploadFiles) {
                        FileInputStream(file).use { fileInput ->
                            zipOutput.putNextEntry(ZipEntry(file.name))
                            var len = 0
                            val buffer = ByteArray(1024)
                            while (fileInput.read(buffer).also { len = it } != -1) {
                                zipOutput.write(buffer, 0, len)
                            }
                            zipOutput.closeEntry()
                        }
                    }
                    zipOutput.finish()
                }
                zipFile
            } else {
                null
            }
        } else {
            null
        }
    }

    /**
     * des: 用于处理上传结果的回调
     * author 秦王
     * time 2021/11/24 14:29
     */
    class ResultCallbackImpl(
        private var cacheDirFile: File,
        private var targetFile: File,
        private var fileUploader: Uploader
    ) : Uploader.ResultCallback {

        // doc: 用于记录重试次数
        private var retryCount = 2

        override fun onUploadSuccess() {
            clear(true)
        }

        private fun clear(success: Boolean = false) {
            isUploading = false
            if (targetFile.exists()) {
                try {
                    targetFile.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (success && cacheDirFile.exists() && cacheDirFile.isDirectory) {
                val logFiles = cacheDirFile.listFiles() ?: return
                for (file in logFiles) {
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        override fun onUploadFail() {
            if (retryCount > 0) {
                retryCount--
                fileUploader.startUpload(targetFile, this)
            } else {
                clear()
            }
        }
    }
}