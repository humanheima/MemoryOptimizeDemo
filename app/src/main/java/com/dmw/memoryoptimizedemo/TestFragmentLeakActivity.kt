package com.dmw.memoryoptimizedemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity


/**
 * Created by p_dmweidu on 2025/2/9
 * Desc: 测试 LeakCanary 检测 Fragment 泄漏
 */
class TestFragmentLeakActivity : AppCompatActivity(), SampleListener {

    companion object {

        fun launch(context: Context) {
            val intent = Intent(context, TestFragmentLeakActivity::class.java)
            context.startActivity(intent)
        }
    }

    private var clRoot: ConstraintLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val fragment = LeakFragment()
        supportFragmentManager.beginTransaction().show(fragment).commit()
    }


    override fun click() {

    }

    override fun onDestroy() {
        //    ListenerManager.getInstance().removeListener(this)
        super.onDestroy()
    }

}
