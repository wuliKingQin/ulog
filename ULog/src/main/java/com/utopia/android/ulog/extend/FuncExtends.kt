package com.utopia.android.ulog.extend

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.os.StatFs
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.utopia.android.ulog.ULog
import com.utopia.android.ulog.print.file.increment.Incrementer
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection

// =========================file function extends===================== \\
/**
 * des: 获取文件或者目录下文件的大小，返回大小单位是字
 * time: 2021/11/18 13:57
 */
internal fun File?.getFileOrDirSize(sumSize: Double = 0.0): Double {
    this ?: return sumSize
    return if (isDirectory) {
        var tempSumSize = sumSize
        listFiles()?.forEach {
            tempSumSize = it.getFileOrDirSize(tempSumSize)
        }
        tempSumSize
    } else {
        val tempSize = sumSize + length().toDouble()
        trace("getFileOrDirSize tempSize=$tempSize")
        tempSize
    }
}

/**
 * des: 获取文件的大小, 单位是兆
 * time: 2021/11/18 14:00
 */
internal fun File?.getFileSize(): Long {
    return getFileOrDirSize().div(1046576).toLong()
}

/**
 * des: 裁剪文件名里面包含的日期
 * time: 2021/11/19 13:43
 */
internal fun File?.splitFileNameInDate(
    separator: String = Incrementer.SEPARATOR,
    suffix: String = Incrementer.FILE_SUFFIX_NAME
): String? {
    this ?: return null
    val namesList= name.split(separator)
    return if (namesList.size >= 2) {
        val fileName = namesList[1]
        val end = fileName.length - suffix.length
        fileName.substring(0, end)
    } else {
        null
    }
}

/**
 * des: 创建新的文件或者目录
 * time: 2021/11/18 10:32
 */
internal fun File?.createNewFileOrDir(): Boolean {
    trace("enter")
    return try {
        this ?: return false
        if (!exists()) {
            try {
                parentFile?.apply {
                    trace("parentFile exists: ${exists()}")
                    if (!exists()) {
                        trace("mkdirs parentFile")
                        mkdirs()
                    }
                }
                val statue = createNewFile()
                trace("create target file statue: $statue")
                statue
            } catch (e: Exception) {
                trace("error: ${e.message}")
                false
            }
        } else {
            trace("target file exits: true")
            false
        }
    } finally {
        trace("end")
    }
}

// ===========================func extends ================== \\
/**
 * des: 获取SD Card剩余大小，单位是字节, 参数是单位转换的除数，可以传1024的倍数
 * time: 2021/11/18 14:49
 */
internal fun getSdCardAvailable(divNumber: Long = 1): Long {
    val state = Environment.getExternalStorageState()
    return if (Environment.MEDIA_MOUNTED == state) {
        var divNum = divNumber.toDouble()
        if (divNum <= 0) {
            divNum = 1.0
        }
        var availableBytes: Long = 0
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                availableBytes = StatFs(Environment.getExternalStorageDirectory().path).availableBytes
            }
            availableBytes = (availableBytes / divNum).toLong()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        availableBytes
    } else {
        0
    }
}

/**
 * des: 用于系统内部的trace记录器
 * time: 2021/11/19 10:08
 */
internal fun trace(trace: String) {
    if (!ULog.isDebug()) return
    val exception = Throwable()
    val className = exception.stackTrace[1].className
    Log.i("ULog", "${className.split(".").lastOrNull()} ${exception.stackTrace[1].methodName}: $trace")
}
// ===========================Long extends ================== \\
/**
 * des: 将长整形格式为日期字符串
 * time: 2021/11/18 15:30
 */
internal fun Long.toDateString(format: String="yyyy-MM-dd HH-mm"): String {
    return SimpleDateFormat(format, Locale.getDefault()).format(Date(this))
}

// ===========================String extends ================== \\
/**
 * des: 请求数据
 * time: 2021/11/24 17:48
 */
internal inline fun String?.request(result: (Boolean, String?)-> Unit) {
    if (this == null) {
        result.invoke(false, null)
        return
    }
    var connection: URLConnection? = null
    try {
        connection = (URL(this).openConnection())?.apply {
            setRequestProperty("Content-Type", "application/json")
            connect()
            val responseCode = if (this is HttpsURLConnection) {
                (this as HttpsURLConnection).responseCode
            } else {
                (this as HttpURLConnection).responseCode
            }
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(inputStream)).use { buffer ->
                    var line: String? = null
                    val resultBuilder = StringBuilder()
                    while (buffer.readLine().also { line = it } != null) {
                        resultBuilder.append(line)
                    }
                    val response = resultBuilder.toString()
                    result.invoke(true, response)
                }
            } else {
                result.invoke(false, null)
            }
        }
        if (connection == null) {
            result.invoke(false, null)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        result.invoke(false, null)
    } finally {
        try {
            if (connection is HttpURLConnection) {
                connection.disconnect()
            } else if (connection is HttpsURLConnection) {
                connection.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * des: 将字符串复制到剪切板
 * time: 2021/11/26 10:33
 */
internal fun String?.copyToClipboard(context: Context, hint: String? = null) {
    this ?: return
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.apply {
        setPrimaryClip(ClipData.newPlainText(null, this@copyToClipboard))
        if (!hint.isNullOrEmpty() && Looper.getMainLooper().thread == Thread.currentThread()) {
            Toast.makeText(context, hint, Toast.LENGTH_SHORT).show()
        }
    }
}