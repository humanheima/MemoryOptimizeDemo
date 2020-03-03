package com.dmw.memoryoptimizedemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity


class SecondActivity : AppCompatActivity(), SampleListener {

    companion object {

        fun launch(context: Context) {
            val intent = Intent(context, SecondActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        ListenerManager.getInstance().addListener(this)
    }


    override fun click() {

    }

    override fun onDestroy() {
    //    ListenerManager.getInstance().removeListener(this)
        super.onDestroy()
    }

}
