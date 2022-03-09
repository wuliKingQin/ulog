package com.utopia.android.ulog.print.file.writer

import com.utopia.android.ulog.extend.createNewFileOrDir
import com.utopia.android.ulog.print.file.encrypt.Encryptor
import com.utopia.android.ulog.tools.UnmapTool
import java.io.*
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList

/**
 * des: 日志库里面默认实现的文件写入器，该文件写入器采用的是java层FileChannel提供的map方法，
 * 建立文件与内存的映射，以实现高速写入文件，并且很好的解决因为程序退出或者崩溃造成的日志丢失问题。
 * 建议使用该默认的文件写入器。
 * author 秦王
 * time 2021/12/24 9:22
 */
class DefaultFileWriter @JvmOverloads constructor(
    private val fileReader: Writer.FileReady? = null,
    override var encryptor: Encryptor? = null
) : Writer {

    companion object {
        // doc: 用于存储缓存的临时目录
        private const val TEMP_DIR = "temp"
        // doc: 日志的临时缓存文件名字
        internal const val TEMP_CACHE_FILE_NAME = "log_temp_cache.log"
        // doc: 用于记录上次写入的日志文件名
        internal const val TEMP_CACHE_LAST_FILE_NAME = "log_temp_file_name.temp"
        // doc: 映射区的最小大小
        private const val MAP_BUFFER_SIZE = 512L
        // doc: 映射区的buffer总大小，当写入的内容达到总大小的3分之一的时候，
        // 将缓存区的内容flush到日志文件
        private const val MAP_BUFFER_TOTAL_SIZE = 150 * 1024L
        /**
         * des: 获取加密后的内容
         * time: 2021/12/25 9:29
         */
        private fun getEncryptByteArray(
            byteArray: ByteArray,
            encryptor: Encryptor?
        ): ByteArray {
            return try {
                encryptor?.onEncrypted(byteArray) ?: byteArray
            } catch (e: Exception) {
                byteArray
            }
        }
    }

    // doc: 临时缓存
    private val mTempCacheList by lazy(LazyThreadSafetyMode.NONE) {
        ArrayList<String>(128)
    }

    private var writeToFileHeadCount = 0
    private var mHeadMessageQueue: LinkedList<String>? = null
    private var mCacheMapOperator: MemoryMapOperator? = null
    private var mLogFileMapOperator: MemoryMapOperator? = null
    private var mCacheFile: File? = null
    private var mLogFile: File? = null
    private var mLastLogFileNameMapOperator: MemoryMapOperator? = null
    override var cacheDir: String? = null
    override fun open(file: File): Boolean {
        return try {
            cacheDir = file.parent
            if (mLogFile == null || mLogFile != file) {
                mLogFile = file
            }
            val isNewFile = mLogFile.createNewFileOrDir()
            val isFlush = checkCacheMapOperator()
            if (isNewFile) {
                onNewFileCreated(file)
            }
            if (isFlush || isNewFile) {
                flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * des: 初始化日志缓存map映射对象
     * time: 2022/2/7 10:23
     */
    private fun checkCacheMapOperator(): Boolean {
        cacheDir ?: return false
        if (mCacheFile == null) {
            mCacheFile = File(File(cacheDir, TEMP_DIR), TEMP_CACHE_FILE_NAME)
            mCacheFile.createNewFileOrDir()
        }
        var isFlush = false
        if (mCacheMapOperator == null) {
            mCacheMapOperator = MemoryMapOperator(
                mCacheFile,
                MAP_BUFFER_TOTAL_SIZE,
                false
            )
            isFlush = true
        }
        return isFlush
    }

    override fun isOpened(): Boolean {
        return mCacheMapOperator != null && (mLogFile?.exists() ?: false)
    }

    override fun getOpenedFile(): File? {
        return if (mLogFile?.exists() == true) mLogFile else null
    }

    override fun getOpenedFileName(): String? {
        return if (mLogFile?.exists() == true) mLogFile?.name else null
    }

    override fun flushMemoryMap() {
        // doc: 如果有缓存的，先把缓存的写入
        if (mTempCacheList.isNotEmpty()) {
            val tempList = ArrayList(mTempCacheList)
            for (msg in tempList) {
                writeToMemoryMap(msg)
            }
            mTempCacheList.clear()
        }
    }

    override fun append(message: String, isWriteTempCache: Boolean) {
        if (isWriteTempCache) {
            writeToTempCache(message)
        } else {
            flushMemoryMap()
            writeToMemoryMap(message)
        }
    }

    /**
     * des: 写到临时缓存
     * time: 2022/3/3 18:39
     */
    private fun writeToTempCache(message: String) {
        mTempCacheList.add(message)
    }

    /**
     * des: 写入共享内存
     * time: 2022/3/3 18:38
     */
    private fun writeToMemoryMap(message: String) {
        checkCacheMapOperator()
        val position = mCacheMapOperator?.getMapPosition() ?: 0
        if (position < MAP_BUFFER_TOTAL_SIZE / 3) {
            val isAppendSuccess = mCacheMapOperator?.append(message, encryptor) ?: false
            if (!isAppendSuccess && mCacheMapOperator != null) {
                flush(message)
            }
        } else {
            flush(message)
        }
    }

    override fun writeToFileHead(message: String) {
        if (mCacheMapOperator?.isEmptyFile() == true) {
            append(message, false)
        } else {
            if (mHeadMessageQueue == null) {
                mHeadMessageQueue = LinkedList()
            }
            if (writeToFileHeadCount <= 0) {
                mHeadMessageQueue?.offerLast(message)
            }
        }
        writeToFileHeadCount ++
    }

    /**
     * des: 获取加密消息
     * time: 2022/2/8 11:30
     */
    private fun getEncryptMessage(message: String?): ByteArray? {
        message ?: return null
        return getEncryptByteArray(message.toByteArray(
            encryptor?.getCharset() ?: Charset.defaultCharset()
        ), encryptor)
    }

    /**
     * des: 将日志信息从缓存刷入到日志文件
     * time: 2021/12/24 13:49
     */
    private fun flush(message: String?) {
        if (!isOpened()) return
        val putFailList = ArrayList<Byte>()
        try {
            val msgByteArray = getEncryptMessage(message)
            val msgByteArraySize = msgByteArray?.size ?: 0
            val lastPosition = mCacheMapOperator?.getLastPosition() ?: 0
            val lineSeparator = if (msgByteArraySize <= 0) {
                UnmapTool.getLineSeparator().toByteArray()
            } else {
                null
            }
            val flushSize = lastPosition + msgByteArraySize.toLong() + (lineSeparator?.size ?: 0)
            if (flushSize <= 0) {
                return
            }
            appendHeadMessage(flushSize, putFailList)
            if (lastPosition > 0) {
                val isAppendSuccess = mLogFileMapOperator?.append(mCacheMapOperator) ?: false
                if (!isAppendSuccess) {
                    val insertArray = mCacheMapOperator?.array()
                    if (insertArray != null) {
                        putFailList.addAll(insertArray.toList())
                    }
                }
            }
            if (msgByteArraySize > 0) {
                msgByteArray.appendLogFile(putFailList)
            }
            if (lineSeparator?.isNotEmpty() == true) {
                lineSeparator.appendLogFile(putFailList)
            }
            mLogFileMapOperator?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (mLogFileMapOperator != null) {
                mLogFileMapOperator?.close()
                mCacheMapOperator?.clear()
            }
            mLogFileMapOperator = null
            if (!putFailList.isNullOrEmpty()) {
                putFailIoWrite(putFailList.toByteArray())
            }
        }
    }

    /**
     * des: 追加头部消息到日志文件
     * time: 2022/2/8 11:44
     */
    private fun appendHeadMessage(flushSize: Long, putFailList: ArrayList<Byte>){
        val isEmptyFile = (mLogFile?.length() ?: 0) <= 0
        if (!isEmptyFile || mHeadMessageQueue.isNullOrEmpty()) {
            mLogFileMapOperator = MemoryMapOperator(
                mLogFile,
                flushSize,
                true
            )
        } else {
            val headMessage = mHeadMessageQueue?.pollFirst()
            val headMsgByteArray = getEncryptMessage(headMessage)
            val headMsgByteArraySize = headMsgByteArray?.size ?: 0
            if (headMsgByteArraySize > 0) {
                mLogFileMapOperator?.close()
                mLogFileMapOperator = MemoryMapOperator(
                    mLogFile,
                    flushSize + headMsgByteArraySize
                )
                headMsgByteArray.appendLogFile(putFailList)
            }
        }
    }

    /**
     * des: 将数据里面的内容追加到日志文件
     * time: 2022/2/8 11:35
     */
    private fun ByteArray?.appendLogFile(putFailList: ArrayList<Byte>) {
        this ?: return
        val isAppendSuccess = mLogFileMapOperator?.append(this) ?: false
        if (!isAppendSuccess) {
            val insertArray = toList()
            putFailList.addAll(insertArray)
        }
    }

    /**
     * des: 写入映射区失败的时候采用io的形式将内容写入文件，最后的底线防止日志丢失
     * time: 2021/12/24 14:35
     */
    private fun putFailIoWrite(byteArray: ByteArray) {
        var bufferWriter: BufferedWriter? = null
        try {
            bufferWriter = BufferedWriter(FileWriter(mLogFile, true))
            val encryptByteArray = getEncryptByteArray(byteArray, encryptor)
            bufferWriter.write(String(
                encryptByteArray,
                encryptor?.getCharset() ?: Charset.defaultCharset()
            ))
            bufferWriter.newLine()
            bufferWriter.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                bufferWriter?.close()
            } catch (e: Exception) {
            }
            bufferWriter = null
        }
    }

    override fun flush() {
        flush(null)
    }

    override fun close(): Boolean {
        flush()
        return true
    }

    override fun onNewFileCreated(file: File) {
        fileReader?.onReady(this)
        writeToFileHeadCount = 0
    }

    override fun getLastLogFileName(cacheDir: String?): String? {
        return getLastLogFileOperator(cacheDir)?.let { operator ->
            try {
                operator.array()?.toString(Charset.defaultCharset())
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun recordLastLogFileName(file: File) {
        getLastLogFileOperator(file.parent)?.apply {
            clear()
            append(file.path)
        }
    }

    /**
     * des: 获取最后保存日志文件映射区的操作对象
     * time: 2021/12/26 9:54
     */
    private fun getLastLogFileOperator(cacheDir: String?): MemoryMapOperator? {
        cacheDir ?: return null
        if (mLastLogFileNameMapOperator == null) {
            mLastLogFileNameMapOperator = MemoryMapOperator(
                File(File(cacheDir, TEMP_DIR), TEMP_CACHE_LAST_FILE_NAME),
                MAP_BUFFER_SIZE,
                false
            )
        }
        return mLastLogFileNameMapOperator
    }

    /**
     * des: 该类封装了文件与内存的映射的操作器，
     * 通过该类可以方便的进行数据的写入到映射区域。
     * author 秦王
     * time 2021/12/24 9:28
     */
    class MemoryMapOperator(
        // doc: 需要映射的文件
        private var mapFile: File?,
        // doc: 映射区域的大小
        private var bufferSize: Long,
        // doc: 是否向映射文件后面追加
        private var isAppend: Boolean = false
    ) : Closeable {

        // doc: 文件和内存建立的映射区域
        private var mMapBuffer: MappedByteBuffer? = null

        // doc: 用于提供FileChannel的文件访问
        private var mRandomAccessFile: RandomAccessFile? = null

        // doc: 用于建立映射的文件通道
        private var mFileChannel: FileChannel? = null

        // doc: 用于清除映射区域的内容，是下次进入的时候保持清洁的空间
        private val mClearBuffer by lazy {
            MappedByteBuffer.allocate(bufferSize.toInt())
        }

        init {
            // doc: 创建操作器的时候就进行内存映射
            getMapBuffer()
        }

        /**
         * des: 追加字节数组数据到映射区域,
         * 追加成功返回true否则返回false
         * time: 2021/12/24 10:04
         */
        fun append(byteArray: ByteArray, encryptor: Encryptor? = null): Boolean {
            return try {
                val encryptByteArray = getEncryptByteArray(byteArray, encryptor)
                getMapBuffer()?.put(encryptByteArray)
                true
            } catch (e: Exception) {
                false
            }
        }

        /**
         * des: 直接将一个映射区域的内存拷贝到当前映射区域,
         * 追加成功返回true否则返回false
         * time: 2021/12/24 10:37
         */
        fun append(operator: MemoryMapOperator?): Boolean {
            operator ?: return false
            return try {
                operator.mMapBuffer?.let { buffer ->
                    buffer.flip()
                    getMapBuffer()?.put(buffer)
                    true
                } ?: false
            } catch (e: Exception) {
                false
            }
        }

        /**
         * des: 追加字符串到映射区域，追加成功返回true否则返回false
         * time: 2021/12/24 10:40
         */
        fun append(message: String, encryptor: Encryptor? = null): Boolean {
            return append(
                message.toByteArray(
                    encryptor?.getCharset() ?: Charset.defaultCharset()
                ),
                encryptor
            )
        }

        /**
         * des: 强制将映射区域的内容同步到文件中
         * time: 2021/12/24 10:51
         */
        fun flush() {
            try {
                mMapBuffer?.force()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * des: 获取文件的内容大小
         * time: 2021/12/24 10:53
         */
        fun getFileSize(): Long {
            return mFileChannel?.size() ?: 0
        }

        /**
         * des: 获取映射区域目前写入内容的位置
         * time: 2021/12/24 10:54
         */
        fun getMapPosition(): Int {
            return mMapBuffer?.position() ?: 0
        }

        /**
         * des: 获取映射区域的Buffer对象，如果没有则创建一个
         * time: 2021/12/24 10:56
         */
        private fun getMapBuffer(): MappedByteBuffer? {
            if (mMapBuffer != null) {
                return mMapBuffer
            }
            if (mapFile?.exists() == false) {
                mapFile.createNewFileOrDir()
            }
            if (mRandomAccessFile == null && mapFile?.exists() == true) {
                mRandomAccessFile = RandomAccessFile(mapFile, "rw")
            }
            if (mFileChannel == null) {
                mFileChannel = mRandomAccessFile?.channel
            }
            if (mMapBuffer == null) {
                mMapBuffer = mFileChannel?.map(
                    FileChannel.MapMode.READ_WRITE,
                    if (isAppend) getFileSize() else 0,
                    bufferSize
                )
            }
            return mMapBuffer
        }

        /**
         * des: 判断是否不是空文件
         * time: 2022/2/8 16:04
         */
        fun isEmptyFile(): Boolean {
            return mMapBuffer?.get(0)?.toChar() == '\u0000'
        }

        /**
         * des: 获取上次写入映射区域的位置，比如第一次建立映射的时候，如果之前映射区有写入的内容，
         * 那么可能在MappedByteBuffer对象里的position得到的是不正确的值，需要进行修正。
         * time: 2021/12/24 11:06
         */
        fun getLastPosition(): Int {
            val contentSize = getFileSize().toInt()
            if (contentSize <= 0) return 0
            val mapPosition = getMapPosition()
            return if (mapPosition > 0) {
                // doc: 表示不是首次，直接返回当前已写入的位置
                mapPosition
            } else {
                // doc: 表示是首次，需要找到上次写入的位置，并将mapBuffer的position进行修正
                val contentEnd = mMapBuffer.findLastPosition(contentSize)
                return if (contentEnd >= 0) {
                    mMapBuffer?.position(contentEnd)
                    contentEnd
                } else {
                    0
                }
            }
        }

        /**
         * des: 这里使用二分法来找到上次一次写入映射区域的最后位置
         * time: 2021/12/24 11:10
         */
        private fun MappedByteBuffer?.findLastPosition(fileSize: Int): Int {
            this ?: return -1
            var left = 0
            var right = fileSize - 1
            var mid = 0
            while (left < right) {
                mid = left + (right - left).shr(1)
                if (get(mid).toChar() == '\u0000') {
                    // doc: 如果找到结尾字符，还需要判断它的前一个是否是正常内容，
                    // 如果是直接结束循环，否则右边前进
                    if (mid - 1 >= 0 && get(mid - 1).toChar() != '\u0000') {
                        mid -= 1
                        break
                    } else {
                        right = mid
                    }
                } else if (mid + 1 < fileSize && get(mid + 1).toChar() == '\u0000') {
                    // doc: 如果当前是正常内容，判断它的后一个是否结束字符，如果是直接结束
                    break
                } else {
                    // doc: 都不满足条件的话，左边前进
                    left = mid + 1
                }
            }
            if (left >= right && get(right).toChar() != '\u0000') {
                // doc: 查找结束，判断结束处是否是正常字符，
                // 是的话需要将right赋值给mid
                mid = right
            }
            return if (mid >= 0 && get(mid).toChar() != '\u0000') {
                mid
            } else {
                -1
            }
        }

        /**
         * des: 用于将文件和内存解除映射关系
         * time: 2021/12/24 10:45
         */
        private fun unmap() {
            try {
                mMapBuffer?.apply {
                    UnmapTool.unmap(this)
                }
                mMapBuffer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun close() {
            unmap()
            try {
                mRandomAccessFile?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mRandomAccessFile = null
            }
            try {
                mFileChannel?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mFileChannel = null
                mapFile = null
            }
        }

        /**
         * des: 用于重置映射区的使用状况
         * time: 2021/12/24 13:53
         */
        fun clear() {
            // doc: 将映射区域的内容进行清零操作
            mMapBuffer?.apply {
                clear()
                try {
                    mClearBuffer.clear()
                    put(mClearBuffer)
                    flush()
                } catch (e: Exception) {
                }
                clear()
            }
        }

        /**
         * des: 获取到
         * time: 2021/12/24 14:26
         */
        fun array(): ByteArray? {
            return try {
                mMapBuffer?.let { buffer ->
                    var size = getLastPosition()
                    if (size <= 0) {
                        null
                    } else {
                        size += 1
                        val array = ByteArray(size)
                        for (index in 0 until size) {
                            array[index] = buffer.get(index)
                        }
                        array
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

        /**
         * des: 用于删除映射文件，
         * 清除之前会先调用close进行映射区域的解除以及关闭文件等
         * time: 2021/12/24 10:42
         */
        fun deleteMapFile() {
            close()
            try {
                mapFile?.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}