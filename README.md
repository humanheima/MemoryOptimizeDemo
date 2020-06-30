Android Studio版本：3.5.1
测试机： HUAWEI LLD-AL00(华为荣耀9青春版）Android版本9。从手机也可以看出来作者也是囊中羞涩啊，哈哈。

推荐先阅读google的官方文章使用 [Memory Profiler 查看 Java 堆和内存分配](https://developer.android.google.cn/studio/profile/memory-profiler.html)

然后啰嗦一下堆转储信息怎么看。
![memory-profiler-dump_2x.png](https://upload-images.jianshu.io/upload_images/3611193-b012cf31145420a1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

在类列表中，您可以查看以下信息：

*   **Allocations**：某个类的实例数量。

Native Size，Shallow Size，Retained Size这几组数据分别意味着什么呢？通过一个例子来说明。

我们用下图来表示某段 Heap Dump 记录的应用内存状态。注意红色的节点，在这个示例中，这个节点所代表的对象从我们的工程中引用了 Native 对象:

![Heap Dump.png](https://upload-images.jianshu.io/upload_images/3611193-8d4f1b5fc522da9a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这种情况不太常见，但在 Android 8.0 之后，使用 Bitmap 便可能产生此类情景，因为 Bitmap 会把像素信息存储在原生内存中来减少 JVM 的内存压力。

**Shallow Size**：这列数据其实非常简单，就是对象本身消耗的内存大小，在上图中，即为红色节点自身所占内存（以字节为单位）。

**Native Size**：同样也很简单，它是类对象所引用的 Native 对象 (蓝色节点) 所消耗的内存大小（以字节为单位）。

![Native Size.png](https://upload-images.jianshu.io/upload_images/3611193-0ea984fb9af65a6b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

**Retained Size**：稍复杂些，它是下图中所有橙色节点的大小（以字节为单位）。

![Retained Size.png](https://upload-images.jianshu.io/upload_images/3611193-0527d89a257fda2e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

由于一旦删除红色节点，其余的橙色节点都将无法被访问，这时候它们就会被 GC 回收掉。从这个角度上讲，它们是被红色节点所持有的，因此被命名为 "Retained Size"。

**注意：默认情况下，此列表按 Retained Size 列排序。要按其他列中的值排序，请点击该列的标题。即我们可以点击`Allocations`或者`Native Size`或者`Shallow Size`或者`Retained Size`进行排序。多次点击某个标题可以选择排序方式。比如说递增排序或者递增排序。**

关于这几列值怎么看，举一个具体的例子。

```
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
```
我们定义一个类，是单链表结构。这个类中有一个40MB的字节数组。40MB计算出来就是41943040。
>1024 * 1024 * 40 = 41943040
1024 * 1024 * 40 * 3 = 125829120

```
class ThirdActivity : AppCompatActivity() {
    
    //head
    private var head: ListItem40MClass? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third)
    }

    //点击3次
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

```
我们点击按钮3次，添加3个ListItem40MClass对象。添加完毕以后，点击一下`Dump Java Heap`按钮，等待Android Studio内置分析工具分析。结果如下。

![step4.png](https://upload-images.jianshu.io/upload_images/3611193-91d7e8af36f7660d.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

Allocations这一列我们可以看到有3个实例对象，没问题。
Native Size这一列忽略，这里没有涉及到Native相关的内存分配。
Shallow Size这一列和Retained Size感觉都不太对啊。先别忙，我们点击ListItem40MClass看看 Instance View面板。
![setp6.png](https://upload-images.jianshu.io/upload_images/3611193-c1dd3b95a56ccc1b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

我们先看Depth这一列，我们可以知道Depth为4的这个对象就是我们的head。Depth为6的这个对象就是链表中最后一个对象。点击这个实例查看详细信息。
![step7.png](https://upload-images.jianshu.io/upload_images/3611193-de40a1b903238bbe.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

我们可以看到链表中最后一个对象的Shallow Sizes是16，说明ListItem40MClass 对象本身只占用16字节，Retained Size值是41943056（16 + 41943040）。而链表中倒数第二个对象的Retained Size是83886112是41943056的两倍。说明在统计Retained Size值的时候不仅统计了对象自身的大小，还加入了引用的对象的大小。也可以看到链表head的Retained Size是125829168正好是41943056的3倍。

我们点击一下Shallow Size，按照Shallow Size值递减排序。

![step9.png](https://upload-images.jianshu.io/upload_images/3611193-39fe920856513f18.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

我们可以看到Shallow Size值比较高的一般都是字节数组，基本数据类型数组，String等类型。

**总结：**
1. 对于Retained Size值：每个对象的Retained Size除了包括自己的大小，还包括引用对象的大小，整个类的Retained Size大小累加起来就大了很多，Retained Size可以用来大概反应哪种类占的内存比较多。
2. 如果想要看整体内存占用，看Shallow Size还是相对准确的，Retained Size可以用来大概反应哪种类占的内存比较多，仅仅是个示意，不过还是Retained Size比较常用，因为Shallow Size的大户一般都是String，数组，基本类型意义不大。

3. 我们在分析的时候可以按照不同的场景可以选择Shallow Size或者Retained Size来进行查看。

### 扯了这么多，现在进入正题。

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

注意：如果某个实例的 **Depth** 为**1**的话，这意味着它直接被 GC root 引用，同时也意味着它永远不会被自动回收。


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
* [Android性能优化（三）之内存管理](https://juejin.im/post/58b18e442f301e0068028a90)
* [使用 Memory Profiler 查看 Java 堆和内存分配](https://developer.android.google.cn/studio/profile/memory-profiler.html)
* [LeakCanary官方文档翻译](https://www.jianshu.com/p/bcaab8f0f280)
* [MarkDown - Latex符号(箭头)的整理](https://blog.csdn.net/m0_37167788/article/details/78603307)
* [Android Studio Profiler Memory （内存分析工具）的简单使用及问题分析](https://blog.csdn.net/happylishang/article/details/86132799)
* [使用 Android Studio Profiler 工具解析应用的内存和 CPU 使用数据](https://mp.weixin.qq.com/s/MIuSi85lnrETqq3rl0Hc6A)

