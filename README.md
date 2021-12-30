# ULog

#### 介绍
该库是使用Kotlin语言开发的Android日志打印库，目前该库只支持Kotlin语言开发的Android项目，单纯的Java语言开发Android项目不支持，除非该项目支持Kotlin语言。

该日志库, 主要功能在于写入文件使用的是FileChannel提供的map方法, 将日志文件和内存进行了映射, 这样极大的提高了写入速度,以及防日志丢失,减少io操作等, 以更加方便的将一些信息写入日志文件,有问题的时候,可以将这些日志信息及时进行上传,提高定位问题的能力.
主要有的功能如下:
1. 支持线上配置功能;
2. 支持日志上传功能;
3. 支持打印到logcat,以及文件.其他打印可以自己实现,每个打印器都跑在自己的线程;
4. 支持写入文件的日志过滤功能;
5. 支持写入文件的日志进行加密功能;
6. 支持默认文件打印器更多配置功能,比如新增文件,清除文件, 日志输出到日志文件的格式等;

#### 引入配置

1.项目根build.gradle配置

```
allprojects {
    repositories {
	...
	maven { url 'https://jitpack.io' }
    }
}
```
2. app目录里的build.gradle配置

```
dependencies {
    ...
    implementation 'com.github.wuliKingQin:ulog:1.0.0'
}
```


#### 使用说明

1. 在Application的onCreate方法里面进行初始化：
```
val config = UConfig.builder()
    .setDebug(BuildConfig.DEBUG)
    .setLogFilePrefixName(packageName)
    .setCacheLogDir(File(externalCacheDri?.absolutePath, "logs").path)
    .addPrinter("自己的实现的打印器或者库里面默认实现的")
    .build()
ULog.init(this, config)
```
2. 如果需要在每个创建的文件里添加首行信息，你需要自己实现 **Writer.FileReady** 接口，并将该接口的实例传给 **DefaultFileWriter** 对象.默认实现的文件写入器采用的是Java层的FileChannel提供的map方法,将日志文件与内存建立映射区, 这样可以防止日志丢失,以及写入文件减少io操作, 并提高写入文件的效率. 样例如下：
```
val fileWriter = DefaultFileWriter(object : Writer.FileReady {
        override fun onReady(writer: Writer) {
              writer.append("==================head content==================\n")
        }
})
val filePrinter = FilePrinter.Builder()
    .setFileWriter(fileWriter)
    .build()
val config = UConfig.Builder()
    ....
    .addPrinter(filePrinter)
    .build()
```
3. 如果需要日志上传功能，那么你需要实现 **Uploader** 接口或者 **AbstractFileUploader** 抽象类并重写startUpload方法,将你的上传结果通过调用resultCallback的onUploadSuccess或者onUploadFail进行返回,如果失败,则进行三次重试,重试都是失败,则删除压缩文件; 如果上传成功,则删除所有的日志文件以及压缩包. 其次还需要实现一个获取上传信息的接口 **Uploader.UploaderInfo**  ，比如：
```
class UploaderImpl(
        uploadInfo: Uploader.UploadInfo
) : AbstractFileUploader(uploadInfo) {
        override fun startUpload(logZipFile: File, resultCallback: Uploader.ResultCallback) {
            Thread.sleep(10000)
            resultCallback.onUploadSuccess()
        }
}
# 实例化文件上传器
val fileUploader = UploaderImpl(object :Uploader.UploadInfo{
        override fun getUploadUrl(): String {
                return ""
        }
        override fun getUniqueIdentity(): String {
                return ""
        }
})
# 向配置中添加上传实现类
val config = UConfig.Builder()
    ...
    .setFileUploader(fileUploader)
    .build()
# 在业务的某个地方进行调用
ULog.uploadToServer("context上下文")
```
4. 如果你需要线上配置控制日志输出的，则需要实现ConfigUpdater接口，同时让你的配置数据类实现ConfigModel接口,比如：
```
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
# 线上配置的使用
val config = UConfig.Builder()
    ...
    .setConfigUpdater(ConfigUploaderImpl())
    .build()
# 根据业务需要,在某个地方调用
ULog.startConfigUpdate("context上下文")
```
5. 如果需要写入文件的日志进行加密, 可以自己实现Encryptor接口, 目前还没有实现默认的加密类.只是提供的接口.加密是对每一条日志进行的加密行为.
