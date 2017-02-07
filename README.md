# Android程序逆向分析

> 最近在看《Android软件安全与逆向分析》，所以把自己的实践过程整理一下。实例选自书中的第二章节，一个最简单的破解案例。分析Android程序是开发Android程序的一个逆向过程。在分析前必须要了解Android开发的流程，程序结构，语句分支，解密原理。


　　分析Android程序是开发Android程序的一个逆向过程。在分析前必须要了解Android开发的流程，程序结构，语句分支，解密原理。

## 0x00 编写一个Android程序
　　具体的编写过程就不做介绍了，源码已经上传github [droid-hack](https://github.com/gnaixx/droid-hack)可以下载实践(module crackme0201)。其中涉及的工具可以在根目录的tools文件夹中找到。
这里贴出关键代码：

```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(R.string.unregister); //未注册
        edit_userName = (EditText) findViewById(R.id.edit_username);
        edit_sn = (EditText) findViewById(R.id.edit_sn);
        btn_register = (Button) findViewById(R.id.button_register);

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkSN(edit_userName.getText().toString().trim(),
                        edit_sn.getText().toString().trim())) {
                    //注册失败
                    Toast.makeText(MainActivity.this,
                            R.string.unsuccessed, Toast.LENGTH_SHORT).show();
                } else {
                    //注册成功
                    Toast.makeText(MainActivity.this,
                            R.string.successed, Toast.LENGTH_SHORT).show();
                    btn_register.setEnabled(false);
                    setTitle(R.string.registered);
                }
            }
        });

    }

    /**
     * 计算用户名和注册码是否匹配
     * @param userName
     * @param sn
     * @return
     */
    private boolean checkSN(String userName, String sn) {
        try {
            if (userName == null || userName.length() == 0) {
                return false;
            }

            if (sn == null || sn.length() == 0) {
                return false;
            }

            //MD5对用户名进行hash 最后计算出SN
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(userName.getBytes());
            byte[] bytes = digest.digest();
            String hexstr = toHexString(bytes, "");
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<hexstr.length(); i+=2){
                sb.append(hexstr.charAt(i));
            }

            String userSN = sb.toString();
            if(!userSN.equalsIgnoreCase(sn)){ //比对SN
                return false;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
```
　　通过比对用户输入的用户名和注册码判断是否注册成功，注册码根据用户名进行MD5后的字符串的下标偶数位组成。

### 编译生成APK
执行效果:
<img width=250px height=400px src="./img/1.jpg"></img>

## 0x01 破解APK
　　通过apktool对APK包进行反编译，生成Smali文件。通过Smali代码找到程序的突破口进行修改，最后使用apktool重新编译并签名。

### 反编译APK
　　我用了最新的apktool版本(2.0.3)进行反编译，工具可以项目的[tools/apktool](https://github.com/gnaix92/crack/tree/master/tools/apktool)目录中找到。
使用非常简单

```shell
apktool d ***.apk -o output
```
生成的反编译文件都在output目录下。
<img width=400px height=200px src="./img/2.png"></img>

### 分析APK文件
　　反编译apk文件成功后，会在当前的output目录下生成一系列目录与文件.其中smali目录下存放了程序所有的反编译代码。res目录是程序的所有资源文件，这些目录的子目录和文件与开发时的源码目录结构是一致的。
　　如何寻找突破口是分析一个程序的关键。对于一般的Android来说，错误提示信息通常是指引关键代码的风向标，错误提示附件一般是核心验证代码。
错误提示是Android程序中的字符串资源，开发Android程序时，这些字符串可能硬编码到源码中，也可能引用自“res/values”目录下的`string.xml`文件，apk文件在打包的时候`string.xml`中的字符串被加密存储在`resources.arsc`文件保存打apk程序中，apk被反编译后这个文件也被解密出来。
　　在源码中当注册失败的时候会Toast提示“无效用户名或注册码”，根据这个线索我们可以打开`res/values/string.xml`文件，找到下面一行：

```xml
<string name="unsuccessed">无效用户名或注册码</string>
```
　　开发Android程序时，`string.xml`文件中的所有字符串资源都在`gen/<packagename>/R.java`文件的`String`类中被标识，每个字符串都有唯一的int类型索引值，使用apktool反编译后，所有索引值保存在`string.xml`文件同目录下的`public.xml`文件中。
以`unsuccessed`为关键字在`public.xml`中搜索可以找到：

```xml
<public type="string" name="unsuccessed" id="0x7f06001f" />
```
`unsuccessed`的id值为`0x7f06001f`, 在smali目录下只有`MainActivity$1.smali`文件一处调用，代码如下:

```smali
# virtual methods
.method public onClick(Landroid/view/View;)V
    .locals 4
    .param p1, "v"    # Landroid/view/View;
    .prologue
    const/4 v3, 0x0
	......
    # invokes: Lcom/example/gnaix/crackme0201/MainActivity;->checkSN(Ljava/lang/String;Ljava/lang/String;)Z
    invoke-static {v0, v1, v2}, Lcom/example/gnaix/crackme0201/MainActivity;->access$200(Lcom/example/gnaix/crackme0201/MainActivity;Ljava/lang/String;Ljava/lang/String;)Z
    move-result v0
    if-nez v0, :cond_0 #如果结果不为0，就跳转到cond_0标号处
    .line 33
    iget-object v0, p0, Lcom/example/gnaix/crackme0201/MainActivity$1;->this$0:Lcom/example/gnaix/crackme0201/MainActivity;
    const v1, 0x7f06001f #unsuccessed字符串
    invoke-static {v0, v1, v3}, Landroid/widget/Toast;->makeText(Landroid/content/Context;II)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    .line 41
    :goto_0
    return-void
    .line 36
    :cond_0
    iget-object v0, p0, Lcom/example/gnaix/crackme0201/MainActivity$1;->this$0:Lcom/example/gnaix/crackme0201/MainActivity;
    const v1, 0x7f06001c  #successed字符串
    invoke-static {v0, v1, v3}, Landroid/widget/Toast;->makeText(Landroid/content/Context;II)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    .line 38
    iget-object v0, p0, Lcom/example/gnaix/crackme0201/MainActivity$1;->this$0:Lcom/example/gnaix/crackme0201/MainActivity;
    # getter for: Lcom/example/gnaix/crackme0201/MainActivity;->btn_register:Landroid/widget/Button;
    invoke-static {v0}, Lcom/example/gnaix/crackme0201/MainActivity;->access$300(Lcom/example/gnaix/crackme0201/MainActivity;)Landroid/widget/Button;
    move-result-object v0
    invoke-virtual {v0, v3}, Landroid/widget/Button;->setEnabled(Z)V
    .line 39
    iget-object v0, p0, Lcom/example/gnaix/crackme0201/MainActivity$1;->this$0:Lcom/example/gnaix/crackme0201/MainActivity;
    const v1, 0x7f06001a
    invoke-virtual {v0, v1}, Lcom/example/gnaix/crackme0201/MainActivity;->setTitle(I)V
    goto :goto_0
.end method

```
　　smali代码中添加注释使用`#`开头在，代码 :

```smali
# invokes: Lcom/example/gnaix/crackme0201/MainActivity;->checkSN(Ljava/lang/String;Ljava/lang/String;)Z
    invoke-static {v0, v1, v2}, Lcom/example/gnaix/crackme0201/MainActivity;->access$200(Lcom/example/gnaix/crackme0201/MainActivity;Ljava/lang/String;Ljava/lang/String;)Z
    move-result v0
    if-nez v0, :cond_0 #如果结果不为0，就跳转到cond_0标号处
```
　　可以看到该处调用了`checkSN()`进行注册码验证，接下去的两行代码表示：第一行将函数的返回值放到**v0**寄存器中，第二行判断**v0**是否不为零，如果是跳转到**cond_0**标号处，反之顺序执行。
在顺序执行中我们可以发现包含了`unsuccessed`字符串:

```smali
 const v1, 0x7f06001f #unsuccessed字符串
```

　　而在`cond_0`下包含了`successed`字符串:

```smali
const v1, 0x7f06001c  #successed字符串
```
　　不难判断当`if-nez v0, :cond_0`条件为真的时候则注册成功，反之注册失败。

### 修改Smali文件代码
　　`if-nez v0, :cond_0`作为程序破解的关键，我们只要将其修改成功能相反的指令即可。`if-nez`是Dalvik指令集中的一个条件跳转指令，类似的还有`if-eqz, if-gez, if-lez`等。与`if-nez`功能相反的是`if-eqz`。
所以我们可以将`if-nez v0, :cond_0`修改为`if-eqz v0, :cond_0`。

## 0x02 重新编译APK并签名
　　这里需要说一点，一开始我重新编译直接报错了,如下:

```shell
I: Using Apktool 2.0.3
I: Smaling smali folder into classes.dex...
I: Building resources...
/Users/xiangqing/Documents/workspace/android/crack/crackme0201/build/outputs/apk/output/res/values-v23/styles.xml:7: error: Error retrieving parent for item: No resource found that matches the given name '@android:style/WindowTitle'.

/Users/xiangqing/Documents/workspace/android/crack/crackme0201/build/outputs/apk/output/res/values-v23/styles.xml:8: error: Error retrieving parent for item: No resource found that matches the given name '@android:style/WindowTitleBackground'.

Exception in thread "main" brut.androlib.AndrolibException: brut.androlib.AndrolibException: brut.common.BrutException: could not exec command: [/var/folders/vr/nlnk4jrj0sz31n6pzdp1h23r0000gp/T/brut_util_Jar_1576083296434469541.tmp, p, --forced-package-id, 127, --min-sdk-version, 8, --target-sdk-version, 23, --version-code, 1, --version-name, 1.0, -F, /var/folders/vr/nlnk4jrj0sz31n6pzdp1h23r0000gp/T/APKTOOL5612206277771121028.tmp, -0, arsc, -0, arsc, -I, /Users/xiangqing/Library/apktool/framework/1.apk, -S, /Users/xiangqing/Documents/workspace/android/crack/crackme0201/build/outputs/apk/output/res, -M, /Users/xiangqing/Documents/workspace/android/crack/crackme0201/build/outputs/apk/output/AndroidManifest.xml]
    at brut.androlib.Androlib.buildResourcesFull(Androlib.java:472)
    at brut.androlib.Androlib.buildResources(Androlib.java:410)
    at brut.androlib.Androlib.build(Androlib.java:298)
    at brut.androlib.Androlib.build(Androlib.java:268)
    at brut.apktool.Main.cmdBuild(Main.java:225)
    at brut.apktool.Main.main(Main.java:84)
Caused by: brut.androlib.AndrolibException: brut.common.BrutException: could not exec command: [/var/folders/vr/nlnk4jrj0sz31n6pzdp1h23r0000gp/T/brut_util_Jar_1576083296434469541.tmp, p, --forced-package-id, 127, --min-sdk-version, 8, --target-sdk-version, 23, --version-code, 1, --version-name, 1.0, -F, /var/folders/vr/nlnk4jrj0sz31n6pzdp1h23r0000gp/T/APKTOOL5612206277771121028.tmp, -0, arsc, -0, arsc, -I, /Users/xiangqing/Library/apktool/framework/1.apk, -S, /Users/xiangqing/Documents/workspace/android/crack/crackme0201/build/outputs/apk/output/res, -M, /Users/xiangqing/Documents/workspace/android/crack/crackme0201/build/outputs/apk/output/AndroidManifest.xml]
    at brut.androlib.res.AndrolibResources.aaptPackage(AndrolibResources.java:425)
    at brut.androlib.Androlib.buildResourcesFull(Androlib.java:458)
    ... 5 more
Caused by: brut.common.BrutException: could not exec command: [/var/folders/vr/nlnk4jrj0sz31n6pzdp1h23r0000gp/T/brut_util_Jar_1576083296434469541.tmp, p, --forced-package-id, 127, --min-sdk-version, 8, --target-sdk-version, 23, --version-code, 1, --version-name, 1.0, -F, /var/folders/vr/nlnk4jrj0sz31n6pzdp1h23r0000gp/T/APKTOOL5612206277771121028.tmp, -0, arsc, -0, arsc, -I, /Users/xiangqing/Library/apktool/framework/1.apk, -S, /Users/xiangqing/Documents/workspace/android/crack/crackme0201/build/outputs/apk/output/res, -M, /Users/xiangqing/Documents/workspace/android/crack/crackme0201/build/outputs/apk/output/AndroidManifest.xml]
    at brut.util.OS.exec(OS.java:89)
    at brut.androlib.res.AndrolibResources.aaptPackage(AndrolibResources.java:419)
    ... 6 more
```
　　推测可能是因为apktool还不能破解sdk 23编译的apk。所以我把 `compileSdkVersion`和`targetSdkVersion`都改为22。同时把`appcompat-v7`改为22版本`compile 'com.android.support:appcompat-v7:22.2.0'`，重新编译顺利通过。
　　不过后来验证只要将`appcompat-v7`改为23版本一下就可以了。具体原因还是不清楚，在作者的[issue](https://github.com/iBotPeaches/Apktool/issues/1171)反馈了。

### 重新编译APK
　　利用apktool重新编译很简单只需要一条简单的命令

```shell
apktool b output
```
<img width=500px height=200px src="./img/3.png"></img>

　　编译成功后会在output目录下生成dist目录，里面有编译成功的apk文件。

### 签名
　　apktool编译的apk并没有签名不能直接安装需要添加签名，用`signapk.jar`工具对apk进行签名。除了`signapk.jar` 还需要`testkey.x509.pem testkey.pk8`两个文件。工具可以在[/tools/auto-sign](https://github.com/gnaix92/crack/tree/master/tools/auto-sign)中找到。运行命令：

```shell
java -jar signapk.jar testkey.x509.pem testkey.pk8 **.apk signgapk.apk
```
　　签名成功后会在根目录下生成signapk.apk。

### 安装测试
　　安装签名好后的apk，输入原来测试的用户名和注册码。可以发现程序注册成功，成功破解。
<img width=250px height=400px src="./img/4.png"></img>


