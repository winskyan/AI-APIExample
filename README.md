# AIKit 语音识别 API 使用指南

本文档介绍如何使用 AIKit 提供的系统级语音识别 API (`AiManager`)。

## 1. 获取 AiManager 实例

应用程序需要通过 `Context` 的 `getSystemService` 方法获取 `AiManager` 的实例。请确保您的应用有权访问此系统服务。

```kotlin
import android.ai.kit.AiManager
import android.content.Context

// 在 Activity 或 Service 中
val aiManager = applicationContext.getSystemService("ai") as? AiManager

if (aiManager == null) {
    // 处理无法获取服务的情况
    Log.e(TAG, "无法获取 AiManager 服务")
    return
}
```

## 2. 监听识别结果

要接收语音识别结果，需要实现 `AiCallback` 接口，并将其注册到 `AiManager`。

### 2.1 实现 AiCallback

```kotlin
import android.ai.kit.AiCallback
import android.util.Log

class MyActivity : AppCompatActivity(), AiCallback {

    companion object {
        const val TAG = "MyActivity"
    }

    // ... 其他 Activity 代码 ...

    override fun onCommandWordRecognized(commandWord: String) {
        // 当识别到命令词时，此方法会被调用
        Log.d(TAG, "识别到的命令词: $commandWord")
        // 在这里处理识别到的命令词，例如更新 UI 或执行操作
    }

    // ... 其他 Activity 代码 ...
}
```

### 2.2 注册和取消注册监听器

在需要开始监听时注册回调，在不需要时（例如 Activity 销毁时）取消注册，以避免内存泄漏。

```kotlin
class MyActivity : AppCompatActivity(), AiCallback {
    private var aiManager: AiManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... 获取 aiManager 实例 ...

        // 在 Activity 创建或恢复时注册
        aiManager?.addListener(this) // this 指向实现了 AiCallback 的类实例
    }

    override fun onDestroy() {
        // 在 Activity 销毁时取消注册
        aiManager?.removeListener(this)
        super.onDestroy()
    }

    // ... 实现 AiCallback 和其他 Activity 代码 ...
}
```

## 3. 管理命令词 (FSA 格式)

API 使用 FSA (Finite State Automaton) 格式来定义可识别的命令词语法。

### 3.1 设置命令词

使用 `setCommandWords` 方法可以添加或更新一个特定场景（由 `key` 标识）的命令词语法。

方法定义如下：

```java
public int setCommandWords(@NonNull String key, @NonNull String fsaContent, @AiConstants.LanguageType int languageType)
```

```kotlin
import android.ai.kit.AiConstants

val key = "key" // 自定义场景 Key
val commandWordFsaContent = """
#FSA 1.0;
0    1    <a>
0    1    -
1    2    <b>
2    3    <c>
;
<a>:打开|关闭;
<b>:客厅|卧室;
<c>:灯|窗帘;
""".trimIndent() // 定义 "打开/关闭 客厅/卧室 的 灯/窗帘"

val languageType = AiConstants.LANGUAGE_TYPE_CHINESE // 或者 AiConstants.LANGUAGE_TYPE_ENGLISH

// 注意：根据方法定义，返回值是 int，表示操作结果码，而非 Boolean
val setResultCode: Int? = aiManager?.setCommandWords(
    key, // 使用 key
    commandWordFsaContent,
    languageType
)

// 假设 0 代表成功，具体需参考 API 定义
if (setResultCode == 0) {
    Log.d(TAG, "命令词设置成功")
} else {
    Log.e(TAG, "命令词设置失败，错误码: $setResultCode")
}
```

**FSA 语法简述:**

- `#FSA 1.0;`: 文件头。
- `0 1 <a>`: 状态转移，表示从状态 0 接收 `<a>` 转移到状态 1。`-` 表示空输入。
- `<a>:打开|关闭;`: 定义符号 `<a>` 可以匹配 "打开" 或 "关闭"。

### 3.2 获取指定语言的所有命令词 FSA 内容

使用 `getAllCommandWordFsaContent` 可以获取当前已为指定语言设置的所有场景的 FSA 内容集合，返回一个以 `key` 为键，FSA 内容为值的 Map。

方法定义如下：

```java
public Map<String, String> getAllCommandWordFsaContent(@AiConstants.LanguageType int languageType)
```

```kotlin
val allFsaContent: Map<String, String>? = aiManager?.getAllCommandWordFsaContent(AiConstants.LANGUAGE_TYPE_CHINESE)

if (allFsaContent != null) {
    for ((key, fsa) in allFsaContent) { // 迭代 key 和 fsa
        Log.d(TAG, "场景 Key [$key]:\n$fsa")
    }
} else {
    Log.e(TAG, "获取所有命令词失败")
}
```

### 3.3 删除指定场景的命令词

使用 `deleteCommandWordFsaContent` 可以删除指定场景（由 `key` 标识）和语言的命令词语法。

方法定义如下：

```java
public int deleteCommandWordFsaContent(@NonNull String key, @AiConstants.LanguageType int languageType)
```

```kotlin
val keyToDelete = "key" // 要删除的场景 Key
// 注意：根据方法定义，返回值是 int，表示操作结果码，而非 Boolean
val deleteResultCode: Int? = aiManager?.deleteCommandWordFsaContent(keyToDelete, AiConstants.LANGUAGE_TYPE_CHINESE)

// 假设 0 代表成功，具体需参考 API 定义
if (deleteResultCode == 0) {
    Log.d(TAG, "场景 Key '$keyToDelete' 的命令词删除成功")
} else {
    Log.e(TAG, "场景 Key '$keyToDelete' 的命令词删除失败，错误码: $deleteResultCode")
}
```

## 4. 常量说明

API 使用 `AiConstants` 类来定义常量。

- `AiConstants.LANGUAGE_TYPE_CHINESE`: 表示中文。
- `AiConstants.LANGUAGE_TYPE_ENGLISH`: 表示英文。

请根据需要选择合适的语言类型。

## 5. 完整示例

一个完整的演示如何在 Activity 中使用 `AiManager` 的示例代码，请参考项目中的以下文件：

[app/src/main/java/com/ai/kit/example/MainActivity.kt](app/src/main/java/com/ai/kit/example/MainActivity.kt)

该文件展示了如何：

- 获取 `AiManager` 实例。
- 实现 `AiCallback` 接口并处理识别结果。
- 在 Activity 生命周期中注册和取消注册监听器。
- 使用按钮触发设置、获取和删除命令词的操作。
