package com.utopia.android.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.utopia.android.demo.databinding.ActivityTestBinding.*
import com.utopia.android.ulog.ULog
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
            executor.execute {
                ULog.d(TAG, "=========日志测试=========")
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