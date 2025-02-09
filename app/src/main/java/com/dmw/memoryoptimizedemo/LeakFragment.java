package com.dmw.memoryoptimizedemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

/**
 * Created by p_dmweidu on 2025/2/9
 * Desc: 泄漏的Fragment
 */
public class LeakFragment extends Fragment {

    private static LeakFragment instance;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this; // 故意造成内存泄漏
    }

}