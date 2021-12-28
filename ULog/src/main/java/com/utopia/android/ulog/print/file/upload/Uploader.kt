package com.utopia.android.ulog.print.file.upload

import java.io.File
import java.io.Serializable

/**
 * des: 抽象一个将日志文件上传到服务器的接口, 实现了所有该接口的类，会运行在UploadLogService的服务里面，
 * 如果进程被杀掉了，那么会暂停上传的操作。此时缓存文件里面会留下压缩包。不过不用担心，下次上传的时候，之前的压缩包会被排除掉。
 * 上传成功后，会自动删除所有的日志文件。
 * author 秦王
 * time 2021/12/25 9:59
 */
interface Uploader: Serializable {

    // doc: 获取上传信息
    var uploadInfo: UploadInfo?
    // doc: 上传提示弹窗的文本信息
    var uploadDialogHintInfo: UploadDialogHintInfo?
    // doc: 上传url
    var uploadUrl: String?
    // doc: 保存唯一身份信息
    var uniqueIdentity: String?

    /**
     * des: 是否应该加入压缩文件，返回1表示需要，返回0表示不需要，-1表示内部实现
     * time: 2021/11/24 17:13
     */
    fun shouldAddToZipFile(targetFile: File): Int {
        return -1
    }

    /**
     * des: 用来修改上传的文件名，不包含后缀
     * time: 2021/11/24 17:16
     */
    fun modifyUploadFileName(): String? = null

    /**
     * des: 开始上传操作
     * time: 2021/11/24 11:34
     */
    fun startUpload(logZipFile: File, resultCallback: ResultCallback)

    /**
     * des: 上传结果回调，用于自定义上传时，实现重试以及删除缓存文件
     * author 秦王
     * time 2021/11/24 14:25
     */
    interface ResultCallback: Serializable {
        /**
         * des: 上传成功
         * time: 2021/11/24 14:27
         */
        fun onUploadSuccess()
        /**
         * des: 上传失败
         * time: 2021/11/24 14:27
         */
        fun onUploadFail()
    }

    /**
     * des: 用于自动上传日志的判断条件，需要接入的项目在需要的位置实现该接口
     * author 秦王
     * time 2021/11/24 11:11
     */
    interface Condition: Serializable {
        /**
         * des: 是否满足条件进行自动上传逻辑
         * time: 2021/11/24 11:12
         */
        fun shouldUpload(userInfoList: List<String>): Boolean
    }

    /**
     * des: 用于获取七牛的token地址，抽象接口
     * time: 2021/11/24 17:35
     */
    interface UploadInfo: Serializable {
        /**
         * des: 上传的请求地址
         * time: 2021/11/24 17:36
         */
        fun getUploadUrl(): String?

        /**
         * des: 获取唯一身份信息，或者设备UUID
         * time: 2021/11/24 18:21
         */
        fun getUniqueIdentity(): String?

        /**
         * des: 灰度身份信息，用于线上控制
         * time: 2021/12/15 18:38
         */
        fun getGrayIdentity(): String?
    }

    /**
     * des: 用于上传修改提示弹窗的信息
     * author 秦王
     * time 2021/12/27 10:40
     */
    interface UploadDialogHintInfo: Serializable {

        /**
         * des: 获取Dialog的提示内容
         * time: 2021/12/27 10:43
         */
        fun getDialogMessage(uniqueIdentity: String): CharSequence

        /**
         * des: 获取Dialog提示的标题，默认是"日志上传提示"
         * time: 2021/12/27 10:44
         */
        fun getDialogTitle(): CharSequence = "日志上传提示"

        /**
         * des: 获取Dialog取消按钮的文本, 默认是”不同意“
         * time: 2021/12/27 10:45
         */
        fun getDialogNegativeText(): CharSequence = "不同意"

        /**
         * des: 获取Dialog确认按钮的文本，默认文本是”不同意“
         * time: 2021/12/27 10:46
         */
        fun getDialogPositiveText(): CharSequence = "不同意"

        /**
         * des: 赋值到剪切板的提示文案，默认是
         * time: 2021/12/27 10:48
         */
        fun getCopyToClipboardToastText(): String = "已经复制到剪切板"
    }
}