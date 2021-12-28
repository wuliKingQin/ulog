package com.utopia.android.ulog.config.online

/**
 * des: 抽象一个在线日志的配置接口，方便接入的项目实现该接口，日志库内部实现大部分主要逻辑
 * author 秦王
 * time 2021/12/25 11:27
 */
interface ConfigModel {

    /**
     * des: 获取白名单用户，用于线上配置指定的用户进行自动上传日志的操作
     * time: 2021/12/27 10:12
     */
    fun getAutoUploadWhiteList(): List<String>?

    /**
     * des: 单个日志文件的大小，如果没有则返回-1
     * time: 2021/12/25 11:35
     */
    fun getMaxLogFileSize(): Int

    /**
     * des: 日志文件夹≥20M，删除最早创建的日志文件，如果没有则返回-1
     * time: 2021/12/25 11:35
     */
    fun getMaxCacheDirSize(): Int

    /**
     * des: 磁盘可用空余≤10M，不再写入日志，如果没有则返回-1
     * time: 2021/12/25 11:36
     */
    fun getRemainingDiskSize(): Int

    /**
     * des: 日志有效期，超过有效期删除，单位:天，如果没有则返回-1
     * time: 2021/12/25 11:36
     */
    fun getLogValidTime(): Int
}