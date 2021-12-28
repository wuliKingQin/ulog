package com.utopia.android.ulog.print.file.upload

/**
 * des: 实现一个上传文件器的抽象类，提供给实现类实现一些基础信息
 * time: 2021/11/26 9:57
 */
abstract class AbstractFileUploader @JvmOverloads constructor(
    override var uploadInfo: Uploader.UploadInfo?,
    override var uploadDialogHintInfo: Uploader.UploadDialogHintInfo? = null
): Uploader {
    override var uploadUrl: String? = null
    override var uniqueIdentity: String? = null
}