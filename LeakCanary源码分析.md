

```java
public void watch(Object watchedReference) {
  watch(watchedReference, "");
}
```

```java

public void watch(Object watchedReference, String referenceName) {
    if (this == DISABLED) {
      return;
    }
    checkNotNull(watchedReference, "watchedReference");
    checkNotNull(referenceName, "referenceName");
    //当前时间
    final long watchStartNanoTime = System.nanoTime();
    String key = UUID.randomUUID().toString();
    retainedKeys.add(key);
    final KeyedWeakReference reference =
        new KeyedWeakReference(watchedReference, key, referenceName, queue);
     
    //调用ensureGoneAsync方法。

    ensureGoneAsync(watchStartNanoTime, reference);
}
```

```java
public final class AndroidRefWatcherBuilder extends RefWatcherBuilder<AndroidRefWatcherBuilder> {
   
  //这里延迟5秒
  private static final long DEFAULT_WATCH_DELAY_MILLIS = SECONDS.toMillis(5);

  @Override protected WatchExecutor defaultWatchExecutor() {
    return new AndroidWatchExecutor(DEFAULT_WATCH_DELAY_MILLIS);
  }


}

```


AndroidWatchExecutor 的 execute 方法。

```java
@Override public void execute(Retryable retryable) {
    
    if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
      //主线程，直接调用waitForIdle
      waitForIdle(retryable, 0);
    } else {
      //非主线程，post消息到主线程
      postWaitForIdle(retryable, 0);
    }
}

void postWaitForIdle(final Retryable retryable, final int failedAttempts) {
    mainHandler.post(new Runnable() {
      @Override public void run() {
        waitForIdle(retryable, failedAttempts);
      }
    });
  }

  void waitForIdle(final Retryable retryable, final int failedAttempts) {
    // This needs to be called from the main thread.
    //主线程空闲的时候处理
    Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
      @Override public boolean queueIdle() {
        postToBackgroundWithDelay(retryable, failedAttempts);
        return false;
      }
    });
  }

```

```java
void postToBackgroundWithDelay(final Retryable retryable, final int failedAttempts) {
    // exponentialBackoffFactor 计算出来是1
    long exponentialBackoffFactor = (long) Math.min(Math.pow(2, failedAttempts), maxBackoffFactor);
    //第一次延迟5秒，多次检测的话，会延迟 10秒，20秒 40秒。。。。
    long delayMillis = initialDelayMillis * exponentialBackoffFactor;
    backgroundHandler.postDelayed(new Runnable() {
      @Override public void run() {
        Retryable.Result result = retryable.run();
        if (result == RETRY) {
          postWaitForIdle(retryable, failedAttempts + 1);
        }
      }
    }, delayMillis);
  }

```







