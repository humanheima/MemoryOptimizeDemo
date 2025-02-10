1. 新建一个文件夹 temp_compiled_res

2. 执行，把 `app/src/main/res` 目录下的资源编译后，输出到 `temp_compiled_res`目录。编译后的资源是 `flat` 结尾的。例如 `mipmap-xxhdpi_ic_launcher.png.flat`

aapt2 compile --dir app/src/main/res   -o temp_compiled_res


//aapt2 link -o output.apk -I /Users/dumingwei/Library/Android/sdk/platforms/android-28/android.jar temp_compiled_res --manifest app/src/main/AndroidManifest.xml -v --stable-ids outputfilename.txt

aapt2 link -o output.apk temp_compiled_res/drawable-v24_ic_launcher_foreground.xml.flat --manifest app/src/main/AndroidManifest.xml --emit-ids outputfilename.txt

3. 使用这个命令， 可以在 [outputfilename.txt](outputfilename.txt) 中，输出id，类似

```xml
com.dmw.memoryoptimizedemo:drawable/ic_launcher_foreground = 0x7f010001
com.dmw.memoryoptimizedemo:drawable/$ic_launcher_foreground__0 = 0x7f010000
```

当我们再次打包，需要稳定id的时候，可以这样。


4. 使用保存的id，outputfilename.txt。在新的 outputfilename2.txt 里面输出新的id
   aapt2 link -o output.apk temp_compiled_res/drawable-v24_ic_launcher_foreground.xml.flat temp_compiled_res/values_colors.arsc.flat --manifest app/src/main/AndroidManifest.xml --stable-ids outputfilename.txt  --emit-ids outputfilename2.txt

新输出的outputfilename2.txt。

```xml
com.dmw.memoryoptimizedemo:color/colorPrimaryDark = 0x7f020002
com.dmw.memoryoptimizedemo:color/colorAccent = 0x7f020000
com.dmw.memoryoptimizedemo:drawable/ic_launcher_foreground = 0x7f010001
com.dmw.memoryoptimizedemo:color/colorPrimary = 0x7f020001
com.dmw.memoryoptimizedemo:drawable/$ic_launcher_foreground__0 = 0x7f010000
```