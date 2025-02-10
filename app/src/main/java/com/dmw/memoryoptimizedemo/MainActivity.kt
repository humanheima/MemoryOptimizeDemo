package com.dmw.memoryoptimizedemo

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * Crete by dumingwei on 2019-10-08
 * Desc:
 *
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    companion object {
        const val MB = 1024 * 1024
    }

    var leakFragment :LeakFragment?=null
    private lateinit var activityManager: ActivityManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.btnGetMemoryInfo -> {
                getMemoryInfo()
            }

            R.id.btnLaunchSecond -> {
                SecondActivity.launch(this)
            }

            R.id.btnLaunchThird -> {
                ThirdActivity.launch(this)
            }

            R.id.btnTestFragmentLeak -> {
                TestFragmentLeakActivity.launch(this)
            }
        }
    }

    private fun getMemoryInfo() {
        //单位MB
        val memoryClass = activityManager.memoryClass
        Log.d(TAG, "onCreate: memoryClass = $memoryClass")

        //单位MB
        val largeMemoryClass = activityManager.largeMemoryClass
        Log.d(TAG, "onCreate: largeMemoryClass = $largeMemoryClass")

        val outInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(outInfo)

        val availMem = outInfo.availMem / MB
        val totalMem = outInfo.totalMem / MB
        val threshold = outInfo.threshold / MB
        val lowMemory = outInfo.lowMemory
        Log.d(TAG, "onCreate: availMem = $availMem")
        Log.d(TAG, "onCreate: totalMem = $totalMem")
        Log.d(TAG, "onCreate: threshold = $threshold")
        Log.d(TAG, "onCreate: lowMemory = $lowMemory")

        //最大分配内存获取方法2
        val maxMemory = (Runtime.getRuntime().maxMemory() / MB)
        //当前分配的总内存
        val totalMemory = (Runtime.getRuntime().totalMemory() / MB)

        Log.d(TAG, "getMemoryInfo: maxMemory = $maxMemory")
        Log.d(TAG, "getMemoryInfo: totalMemory = $totalMemory")

    }
}
