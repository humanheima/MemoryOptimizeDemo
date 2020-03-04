package com.dmw.memoryoptimizedemo

/**
 * Crete by dumingwei on 2020-03-04
 * Desc:
 *
 */
class ListItem40MClass {

    // 40MB
    // 1024 * 1024 * 40 = 41943040
    var content = ByteArray(1024 * 1024 * 40)


    init {
        for (i in content.indices) {
            content[i] = 1
        }
    }

    var next: ListItem40MClass? = null

}


