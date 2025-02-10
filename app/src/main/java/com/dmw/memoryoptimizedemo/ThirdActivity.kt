package com.dmw.memoryoptimizedemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * Crete by dumingwei on 2020-03-04
 * Desc:
 *
 */
class ThirdActivity : AppCompatActivity() {


    companion object {

        fun launch(context: Context) {
            val intent = Intent(context, ThirdActivity::class.java)
            context.startActivity(intent)
        }
    }

    private var head: ListItem40MClass? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third)
    }

    /**
     * 点击3次
     */
    fun onClick(view: View) {
        when (view.id) {
            R.id.btnAddNode -> {
                addNode()
            }
        }
    }

    private fun addNode() {
        if (head == null) {
            head = ListItem40MClass()
        } else {
            var tmp = head
            while (tmp?.next != null) {
                tmp = tmp.next
            }
            tmp?.next = ListItem40MClass()
        }

    }
}
