package com.dmw.memoryoptimizedemo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * Created by p_dmweidu on 2025/2/9
 * Desc: 泄漏的Fragment
 */
class LeakFragment : Fragment() {

    private val TAG = "LeakFragment"

    companion object {
        private var instance: LeakFragment? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this // 故意造成内存泄漏
//        handler.postDelayed({
//            Log.d(TAG, "onCreate: instance =$instance")
//            instance = this // 故意造成内存泄漏
//
//        }, 20)
        Log.d(TAG, "onCreate: ")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_leak, container, false)
    }

}