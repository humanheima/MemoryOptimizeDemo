package com.dmw.memoryoptimizedemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout


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
        setContentView(R.layout.activity_fragment_leak)

        val fragment = LeakFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.cl_root_layout, fragment)
            .commit()
    }


    override fun click() {

    }

    override fun onDestroy() {
        //    ListenerManager.getInstance().removeListener(this)
        super.onDestroy()
    }

}
