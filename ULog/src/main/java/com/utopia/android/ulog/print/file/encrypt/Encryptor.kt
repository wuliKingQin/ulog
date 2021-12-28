package com.utopia.android.ulog.print.file.encrypt

import java.nio.charset.Charset

/**
 * des: 抽象一个加密器接口，对日志信息进行加密处理, 默认实现@DefaultEncryptor
 * author 秦王
 * time 2021/12/25 9:09
 */
interface Encryptor {

    /**
     * des: 用于将字符串转字节数组使用编码，默认是返回UTF-8
     * time: 2021/12/25 9:20
     */
    fun getCharset(): Charset {
        return Charset.defaultCharset()
    }

    /**
     * des: 在该方法中实现加密并返回加密后的内容
     * time: 2021/12/25 9:07
     */
    fun onEncrypted(rawMsg: ByteArray): ByteArray
}