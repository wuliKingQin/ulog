package com.utopia.android.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.utopia.android.demo.databinding.ActivityTestBinding.*
import com.utopia.android.ulog.ULog
import java.lang.Exception
import java.util.concurrent.Executors

class TestActivity: AppCompatActivity(){

    companion object {
        private const val TAG = "TestActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = inflate(layoutInflater).also {
            setContentView(it.root)
        }
        val executor = Executors.newCachedThreadPool()
        binding.testButton.setOnClickListener {
//            executor.execute {
//                for (index in 0 until 5) {
//                    ULog.d(TAG, "index=${index} ========测试日志信息=========")
//                }
//            }
//            executor.execute {
//                for (index in 5 until 10) {
//                    ULog.d(TAG, "index=${index} ========测试日志信息=========")
//                }
//            }
//            executor.execute {
//                for (index in 10 until 15) {
//                    ULog.d(TAG, "index=${index} ========测试日志信息=========")
//                }
//            }
            try {
                1 / 0
            } catch (e: Exception) {
                ULog.d(TAG, e)
            }
        }
        binding.updateConfigBtn.setOnClickListener {
            ULog.startConfigUpdate(this)
        }
        binding.uploadFileBtn.setOnClickListener {
            ULog.uploadToServer(this)
        }
    }
}