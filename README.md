Android Studio版本：3.5.1
测试机： HUAWEI LLD-AL00(华为荣耀9青春版）Android版本9。从手机也可以看出来作者也是囊中羞涩啊，哈哈。

推荐先阅读google的官方文章使用 [Memory Profiler 查看 Java 堆和内存分配](https://developer.android.google.cn/studio/profile/memory-profiler.html)

然后想重复一下堆转储信息怎么看。
![memory-profiler-dump_2x.png](https://upload-images.jianshu.io/upload_images/3611193-b012cf31145420a1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

在类列表中，您可以查看以下信息：

*   **Allocations**：堆中的分配数。
*   **Native Size**：此对象类型使用的原生内存总量（以字节为单位）。只有在使用 Android 7.0 及更高版本时，才会看到此列。您会在此处看到采用 Java 分配的某些对象的内存，因为 Android 对某些框架类（如 [Bitmap](https://developer.android.google.cn/reference/android/graphics/Bitmap)）使用原生内存。
*   **Shallow Size**：此对象类型使用的 Java 内存总量（以字节为单位）。
*   **Retained Size**：为此类的所有实例而保留的内存总大小（以字节为单位）。

**注意：默认情况下，此列表按 Retained Size 列排序。要按其他列中的值排序，请点击该列的标题。即我们可以点击`Allocations`或者`Native Size`或者`Shallow Size`或者`Retained Size`进行排序。多次点击某个标题可以选择排序方式，比如说递增排序或者递增排序。**



我们模拟一个内存泄漏的场景：

定义一个监听接口SampleListener
```
interface SampleListener {

    void click();
}

```
定义一个监听管理类ListenerManager，用来添加和删除SampleListener。我们需要创建一个ListenerManager单例类。

```
public class ListenerManager {

    //静态对象
    private static ListenerManager sInstance;

    private List<SampleListener> listeners = new ArrayList<>();

    private ListenerManager() {
    }

    public static synchronized ListenerManager getInstance() {
        if (sInstance == null) {
            sInstance = new ListenerManager();
        }

        return sInstance;
    }

    public void addListener(SampleListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SampleListener listener) {
        listeners.remove(listener);
    }
}

```

然后让SecondActivity实现SampleListener接口，在onCreate方法中注册监听。
```
class SecondActivity : AppCompatActivity(), SampleListener {
   
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        ListenerManager.getInstance().addListener(this)
    }

    override fun click() {

    }

}
```
多次打开SecondActivity，因为我们只注册了监听，但是没有取消注册，所以会导致ListenerManager类型的静态实例`sInstance`持有多个SecondActivity实例。造成内存泄漏。我们使用Android Studio自带的Memory Profiler 查看 Java 内存信息并进行堆转储。获取堆转储信息以后Android Studio会自动帮我们分析堆转储信息，如下图所示。

![setp1.jpg](https://upload-images.jianshu.io/upload_images/3611193-f9d75f05de484685.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


我们可以看到现在内存中有7个SecondActivity实例，点击SecondActivity类，查看对应的Instance View面板。
![step2.png](https://upload-images.jianshu.io/upload_images/3611193-48a21239913b1b15.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


然后我们点击一个实例对象，看看它的引用路径。

![step3.jpg](https://upload-images.jianshu.io/upload_images/3611193-81e415e3298e03b5.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


我们重点关注一下Depth，Depth表示从任意GC根到选定实例的最短跳数。在这个例子中从GC根到泄漏的SecondActivity实例的最短路径是4。

GC根：类静态变量sInstance
$\downarrow$
ListenerManager类中的listeners
$\downarrow$
ArrayList中的`Object[] elementData`
$\downarrow$
`Object[] elementData`数组中index为5的对象
$\downarrow$
泄漏的SecondActivity实例`SecondActivity@315745280`


泄漏的原因就是这条路径上的某个对象造成的，我们需要仔细分析这条路径上的对象，来判断到底泄漏发生的原因。在这个例子中静态变量`sInstance`无法被回收，那么`sInstance`中的成员变量`listeners`也无法被回收，因为`listeners`又持有SecondActivity的引用，所以最终导致SecondActivity无法被回收造成内存泄漏。

在这个例子中，我们可以在SecondActivity的onDestroy方法中移除监听就能解决泄漏问题。
```
override fun onDestroy() {
    ListenerManager.getInstance().removeListener(this)
    super.onDestroy()
}
```

### 使用LeakCanary

使用LeakCanary来检测内存泄漏也是美滋滋。直接在app的build.gradle文件中添加LeakCanary的依赖就完事了。

```
debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.0-beta-4'
```

然后我们也多次打开SecondActivity，当LeakCanary检测到内存泄露以后会弹出通知，我们点击通知即可查看泄漏信息。

![leacanary.jpg](https://upload-images.jianshu.io/upload_images/3611193-dd0efc66fa9910e4.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


我们要怎么从这个GC路径中找到造成泄漏发生的对象呢？

首先我们从上往下看找到最后一个是否泄漏为NO（Leaking: NO）的对象。在这个图中是：ListenerManager类型的对象sInstance。
![leakcanary1.png](https://upload-images.jianshu.io/upload_images/3611193-cac021a511679728.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

然后我们继续往下看找到第一个是否泄漏为YES（Leaking: YES）的对象。在这个图中是：SecondActivity实例
![leakcanary2.png](https://upload-images.jianshu.io/upload_images/3611193-37f3dca3b3cb588f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

我们可以推断泄漏是由最后一个没有泄漏的对象（Leaking: NO ）和第一个泄漏的对象（Leaking: YES）之间的对象导致的，是我们重点排查的对象，如下图所示：
![leacanary3.jpeg](https://upload-images.jianshu.io/upload_images/3611193-006499e04f061f9e.jpeg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

我们经过排查可以发现，造成泄漏的原因就是SecondActivity作为SampleListener加入到静态对象sInstance的listeners集合中，然后在SecondActivity`onDestroy`以后，listeners依然持有SecondActivity的引用，导致SecondActivity无法被回收。


参考链接：
1. [Android性能优化（三）之内存管理](https://juejin.im/post/58b18e442f301e0068028a90)
2. [使用 Memory Profiler 查看 Java 堆和内存分配](https://developer.android.google.cn/studio/profile/memory-profiler.html)
3. [LeakCanary官方文档翻译](https://www.jianshu.com/p/bcaab8f0f280)
4. [MarkDown - Latex符号(箭头)的整理](https://blog.csdn.net/m0_37167788/article/details/78603307)
5. [Android Studio Profiler Memory （内存分析工具）的简单使用及问题分析](https://blog.csdn.net/happylishang/article/details/86132799)



