package com.utopia.android.ulog.print.file.upload

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import com.utopia.android.ulog.config.UConfig
import com.utopia.android.ulog.extend.copyToClipboard
import com.utopia.android.ulog.extend.trace
import java.lang.Exception
import java.lang.StringBuilder

/**
 * des: 用于封装通用的上传日志逻辑
 * time: 2021/11/26 10:40
 */
class UploadHandler {
    /**
     * des: 用于判断是否需要上传提示Dialog
     * time: 2021/11/26 10:48
     */
    private var isNeedHintDialog = true

    /**
     * des: 上传日志文件到服务器
     * time: 2021/11/24 11:23
     */
    fun startUpload(
        context: Context,
        config: UConfig,
        uploadCondition: Uploader.Condition?,
        isSilentlyUpload: Boolean,
        isCheckUseInfo: Boolean
    ) {
        trace("enter")
        isNeedHintDialog = !isSilentlyUpload
        try {
            trace("UploadLogService.isUploading=${UploadJobService.isUploading}")
            if (UploadJobService.isUploading) {
                // doc: 防止重复执行上传逻辑
                trace("uploading")
                return
            }
            UploadJobService.isUploading = true
            var isUploadToService = true
            if (isCheckUseInfo) {
                val condition = uploadCondition ?: DefaultUploadFilter().apply {
                    this.uniqueIdentity = uniqueIdentity ?:
                    config.fileUploader?.uploadInfo?.getUniqueIdentity()
                }
                val userInfoList = config.getOnlineConfig()?.getAutoUploadWhiteList()
                if (userInfoList.isNullOrEmpty()) {
                    UploadJobService.isUploading = false
                    trace("The upload condition is not met===logAutoUploadList is null")
                    return
                }
                isUploadToService = condition.shouldUpload(userInfoList)
            }
            trace("isUploadToService=${isUploadToService}")
            if (isUploadToService) {
                doUpload(context, config)
            } else {
                UploadJobService.isUploading = false
            }
        } finally {
            trace("end")
        }
    }

    /**
     * des: 做上传日志的任务
     * time: 2021/11/24 14:08
     */
    private fun doUpload(
        context: Context,
        config: UConfig) {
        trace("enter")
        val cacheDir = config.cacheLogDir ?: return
        val fileUploader = config.fileUploader
        trace("fileUploader=$fileUploader")
        if (fileUploader == null) {
            trace("fileUploader == null")
            UploadJobService.isUploading = false
            return
        }
        val tempUniqueIdentity = fileUploader.uniqueIdentity
        if (isNeedHintDialog && context is Activity && !tempUniqueIdentity.isNullOrEmpty()) {
            uploadHintDialog(context, fileUploader, cacheDir, tempUniqueIdentity)
        } else {
            startUpload(context, cacheDir)
        }
        trace("end")
    }

    /**
     * des: 上传dialog提示
     * time: 2021/11/26 11:26
     */
    private fun uploadHintDialog(context: Context, fileUploader: Uploader, cacheDir: String, uniqueIdentity: String) {
        if (fileUploader.uploadDialogHintInfo == null) {
            fileUploader.uploadDialogHintInfo = DefaultUploadDialogHintInfoImpl()
        }
        val dialogHintInfo = fileUploader.uploadDialogHintInfo!!
        val hintDialog = AlertDialog.Builder(context)
            .setMessage(dialogHintInfo.getDialogMessage(uniqueIdentity))
            .setCancelable(false)
            .setTitle(dialogHintInfo.getDialogTitle())
            .setNegativeButton(dialogHintInfo.getDialogNegativeText()) { _, _ ->
                UploadJobService.isUploading = false
            }
            .setPositiveButton(dialogHintInfo.getDialogPositiveText()) { _, _ ->
                startUpload(context, cacheDir)
                uniqueIdentity.copyToClipboard(context, dialogHintInfo.getCopyToClipboardToastText())
            }
            .create()
        hintDialog.show()
    }

    /**
     * des: 开始上传日志文件
     * time: 2021/11/26 9:37
     */
    private fun startUpload(context: Context, cacheDir: String) {
        trace("enter")
        try {
            UploadJobService.startUpload(context, cacheDir)
        } catch (e: Exception) {
            e.printStackTrace()
            UploadJobService.isUploading = false
        }
        trace("end")
    }

    /**
     * des: 默认上传条件过滤器
     * author 秦王
     * time 2021/11/30 16:35
     */
    class DefaultUploadFilter(
         var uniqueIdentity: String? = null
    ): Uploader.Condition {
        override fun shouldUpload(userInfoList: List<String>): Boolean {
            return userInfoList.contains(uniqueIdentity)
        }
    }

    /**
     * des: 默认实现提示弹窗的文案信息
     * author 秦王
     * time 2021/12/27 10:50
     */
    class DefaultUploadDialogHintInfoImpl: Uploader.UploadDialogHintInfo {
        override fun getDialogMessage(uniqueIdentity: String): CharSequence {
            return StringBuilder()
                .append("是否同意将日志上传到服务器，如果同意请将以下信息发送给对接人员:\n\n")
                .append(uniqueIdentity)
                .append("\n\n")
                .append("注意：点击立即上传会自动将以上信息复制到剪切板哦！")
        }
    }
}