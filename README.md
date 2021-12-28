# ULog

#### 介绍
该库是使用Kotlin语言开发的Android日志打印库，目前该库只支持Kotlin语言开发的Android项目，单纯的Java语言开发Android项目不支持，除非该项目支持Kotlin语言。

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
    implementation 'com.gitee.wuliKingQin:ulog:v0.0.1-beta'
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
2. 如果需要在每个创建的文件里添加首行信息，你需要自己实现 **Writer.FileReady** 接口，并将该接口的实例传给 **DefaultFileWriter** 对象，比如：
```
val fileWriter = DefaultFileWriter(object : Writer.FileReady {
        override fun onReady(writer: Writer) {
              writer.append("==================head content==================\n")
        }
})
val filePrinter = FilePrinter.Builder()
    .setFileWriter(fileWriter)
    .build()
```
3. 如果需要日志上传功能，那么你需要实现 **Uploader** 接口或者 **AbstractFileUploader** 抽象类，其次还需要实现一个获取上传信息的接口 **Uploader.UploaderInfo**  ，比如：
```
class UploaderImpl(
        uploadInfo: Uploader.UploadInfo
) : AbstractFileUploader(uploadInfo) {
        override fun startUpload(logZipFile: File, resultCallback: Uploader.ResultCallback) {
            Thread.sleep(10000)
            resultCallback.onUploadSuccess()
        }
}
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
```
