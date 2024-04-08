LeakCanary 是一个用于检测 Android 和 Java 应用内存泄漏的开源库。它的实现原理主要基于 Java 的弱引用和引用队列。

LeakCanary 是怎么检测 Activity 泄漏的？

1. LeakCanary 通过监听 Activity 的生命周期来检测 Activity 泄漏。
2. 在 Activity 被销毁时,创建一个被 Activity 对象的弱引用 KeyedWeakReference ，并和一个弱引用队列(queue = ReferenceQueue<Any>())关联。
3. 将被监视对象的弱引用添加到 `watchedObjects = mutableMapOf<String, KeyedWeakReference>()` map中。
4. 延迟5秒执行检查操作。这里使用了一个单线程的 Executor 来执行检查操作。这样可以保证检查操作的执行顺序，避免并发问题。如果检查的时候发现对象对应的弱引用(也就是 KeyedWeakReference 被添加到 queue)，说明对象马上就要被回收了，就不需要再监视它了。从 watchedObjects 中移除。
5. 如果弱引用对象还在 watchedObjects 列表中，说明对象还没有被回收，通知监听器进行dump heap 和分析。
6. 调用 HeapDumper.dumpHeap 方法来获取堆信息。如果获取失败，延迟5秒后，再次dump heap。
7. 调用 HeapAnalyzerService.runAnalysis 方法来分析堆信息。

### 源码分析

LeakCanary 检测 Activity 泄漏的方式主要是通过监听 Activity 的生命周期。具体来说，它在 Activity 被销毁时，将其添加到一个被监视的对象列表中。然后，LeakCanary 会定期检查这个列表中的对象是否已经被垃圾回收。如果某个对象在一段时间后仍然没有被垃圾回收，那么 LeakCanary 就会认为这个对象可能发生了内存泄漏。

在你提供的代码中，`ActivityDestroyWatcher` 类就是用来监听 Activity 生命周期的。当 Activity 被销毁时，`onActivityDestroyed` 方法会被调用，然后将 Activity 对象传递给 `ObjectWatcher` 的 `watch` 方法进行监视：

```kotlin
override fun onActivityDestroyed(activity: Activity) {
  if (configProvider().watchActivities) {
    objectWatcher.watch(activity)
  }
}
```

`ObjectWatcher` 类是 LeakCanary 的核心部分，它负责监视对象并检测可能的内存泄漏。当调用 `watch` 方法时，`ObjectWatcher` 会创建一个 `KeyedWeakReference` 对象来弱引用待监视的对象，并将这个弱引用添加到 `watchedObjects` 列表中：

```kotlin
@Synchronized fun watch(
  watchedObject: Any,
  name: String
) {
  if (!isEnabled()) {
    return
  }
  //注释0处，在监听之前，先调用 removeWeaklyReachableObjects 方法来检查 watchedObjects 列表中的对象是否已经被垃圾回收，然后将已经被回收的对象从 watchedObjects 列表中移除  
  removeWeaklyReachableObjects()
  val key = UUID.randomUUID().toString()
  val watchUptimeMillis = clock.uptimeMillis()
  //注释1处，创建被监视对象的弱引用，并和一个弱引用队列 queue 关联
  val reference = KeyedWeakReference(watchedObject, key, name, watchUptimeMillis, queue)
  //注释2处，将被监视对象的弱引用添加到 watchedObjects 列表中  
  watchedObjects[key] = reference
  //注释3处，执行检查操作  
  checkRetainedExecutor.execute {
    moveToRetained(key)
  }
}
```

注释0处，在监听之前，先调用 removeWeaklyReachableObjects 方法来检查 watchedObjects 列表中的对象是否已经被垃圾回收，然后将已经被回收的对象从 watchedObjects 列表中移除。

```java
private fun removeWeaklyReachableObjects() {
    // WeakReferences are enqueued as soon as the object to which they point to becomes weakly
    // reachable. This is before finalization or garbage collection has actually happened.
    // 只要被监视对象变得弱可达，其弱引用就会被加入到引用队列中。这在对象被回收之前就会发生。    
    var ref: KeyedWeakReference ?
        do {
            ref = queue.poll() as KeyedWeakReference ?
                if(ref != null) {
                    //注释1处，被监视对象的弱引用已经被加入到引用队列，说明该对象已经变得弱可达，从 watchedObjects 列表中移除。
                    watchedObjects.remove(ref.key)
                }
        } while (ref != null)
}
```

注释1处，只要被监视对象变得弱可达，其弱引用就会被加入到引用队列中。这在对象被回收之前就会发生。说明这个弱引用包装的对象马上就要被回收了，很好，那么就不需要监视它了。从 watchedObjects 列表中移除。

watch 方法注释1处，创建被监视对象的弱引用，并和一个弱引用队列 queue 关联。那么当垃圾回收器准备回收一个被 KeyedWeakReference 包装的对象时，该 KeyedWeakReference 引用会被加入到关联的ReferenceQueue。

注释2处，将被监视对象的弱引用添加到 watchedObjects 列表中。这样，LeakCanary 就可以定期检查这个列表中的对象是否已经被垃圾回收。

注释3处，执行检查操作。这里使用了一个单线程的 Executor 来执行检查操作。这样可以保证检查操作的执行顺序，避免并发问题。

默认情况下 checkRetainedExecutor 就是 InternalAppWatcher 类中的 checkRetainedExecutor 对象。

```kotlin
private val checkRetainedExecutor = Executor {
mainHandler.postDelayed(it, AppWatcher.config.watchDurationMillis)
}
```

当 checkRetainedExecutor 执行的时候，就是延迟了 AppWatcher.config.watchDurationMillis(默认是5秒) 时间后执行了 ObjectWatcher 的 moveToRetained 方法。

ObjectWatcher 的 moveToRetained 方法。
```kotlin
@Synchronized private fun moveToRetained(key: String) {
    //先尝试移除
    removeWeaklyReachableObjects()
    val retainedRef = watchedObjects[key]
    if(retainedRef != null) {
        //5秒后，如果对象还在 watchedObjects 列表中，说明对象还没有被回收，通知监听器进行操作
        retainedRef.retainedUptimeMillis = clock.uptimeMillis()
        onObjectRetainedListeners.forEach {
            it.onObjectRetained()
        }
    }
}
```

5秒后，如果对象还在 watchedObjects 列表中，说明对象还没有被回收，通知监听器进行操作。

InternalLeakCanary 的 onObjectRetained 方法。

```kotlin
override fun onObjectRetained() {
    scheduleRetainedObjectCheck("ObjectWatcher")
}
``` 

scheduleRetainedObjectCheck 方法。

```kotlin
private fun scheduleRetainedObjectCheck(reason: String) {
    if(checkScheduled) {
        SharkLog.d {
            "Already scheduled retained check, ignoring ($reason)"
        }
        return
    }
    checkScheduled = true
    backgroundHandler.post {
        checkScheduled = false
        checkRetainedObjects(reason)
    }
}
```

checkRetainedObjects 方法。

```kotlin
private fun checkRetainedObjects(reason: String) {
    val config = configProvider()
        // A tick will be rescheduled when this is turned back on.
    if(!config.dumpHeap) {
        SharkLog.d {
            "No checking for retained object: LeakCanary.Config.dumpHeap is false"
        }
        return
    }
    SharkLog.d {
        "Checking retained object because $reason"
    }

    //如果有保留的对象，先执行一次 GC
    var retainedReferenceCount = objectWatcher.retainedObjectCount
    if(retainedReferenceCount > 0) {
        gcTrigger.runGc()
        retainedReferenceCount = objectWatcher.retainedObjectCount
    }

    //执行GC后，如果保留的对象数量小于阈值，默认前台是5，不进行检查
    if(checkRetainedCount(retainedReferenceCount, config.retainedVisibleThreshold)) return

    if(!config.dumpHeapWhenDebugging && DebuggerControl.isDebuggerAttached) {
        showRetainedCountWithDebuggerAttached(retainedReferenceCount)
        scheduleRetainedObjectCheck("debugger was attached", WAIT_FOR_DEBUG_MILLIS)
        SharkLog.d {
            "Not checking for leaks while the debugger is attached, will retry in $WAIT_FOR_DEBUG_MILLIS ms"
        }
        return
    }

    SharkLog.d {
        "Found $retainedReferenceCount retained references, dumping the heap"
    }
    val heapDumpUptimeMillis = SystemClock.uptimeMillis()
    KeyedWeakReference.heapDumpUptimeMillis = heapDumpUptimeMillis
    dismissRetainedCountNotification()
    //注释1处，调用 HeapDumper.dumpHeap 方法来获取堆信息
    val heapDumpFile = heapDumper.dumpHeap()
    if(heapDumpFile == null) {
        SharkLog.d {
            "Failed to dump heap, will retry in $WAIT_AFTER_DUMP_FAILED_MILLIS ms"
        }
        //注释2处，如果 dumpHeap 失败，延迟 WAIT_AFTER_DUMP_FAILED_MILLIS 后再次执行检查操作
        scheduleRetainedObjectCheck("failed to dump heap", WAIT_AFTER_DUMP_FAILED_MILLIS)
        showRetainedCountWithHeapDumpFailed(retainedReferenceCount)
        return
    }
    lastDisplayedRetainedObjectCount = 0
    objectWatcher.clearObjectsWatchedBefore(heapDumpUptimeMillis)
    //注释2处，调用 HeapAnalyzerService.runAnalysis 方法来分析堆信息
    HeapAnalyzerService.runAnalysis(application, heapDumpFile)
}
```

注释1处，调用 HeapDumper.dumpHeap 方法来获取堆信息。

注释2处，如果 dumpHeap 失败，延迟 WAIT_AFTER_DUMP_FAILED_MILLIS 后再次执行checkRetainedObjects，生成堆信息文件，然后调用 HeapAnalyzerService.runAnalysis 方法来分析堆信息。

注释3处，调用 HeapAnalyzerService.runAnalysis 方法来分析堆信息。



* [LeakCanary官方文档翻译](https://www.jianshu.com/p/bcaab8f0f280)