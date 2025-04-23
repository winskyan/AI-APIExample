# AI 服务 API 使用指南

本文档介绍如何使用 AI 提供的系统级AI服务 API (`AiManager`)。

## 获取 SDK (ai-api.jar)

要编译使用 `AiManager` API 的应用程序，您需要获取 `ai-api.jar` 文件。这个 JAR 文件包含了必要的接口定义（如 `AiManager`, `AiCallback`）和常量（如 `AiConstants`），使得您的代码可以在开发环境中引用这些类。

**使用方法**：

1. **获取 JAR 文件**：`ai-api.jar` 文件可以从本项目的 `app/src/main/libs/` 目录下找到并复制。
2. **放置 JAR 文件**：将获取到的 `ai-api.jar` 文件放置在您的应用模块下的 `libs` 目录中（例如：`app/libs/ai-api.jar`）。如果 `libs` 目录不存在，请创建它。
3. **配置依赖**：在您的应用模块的 `build.gradle.kts` (或 `build.gradle`) 文件中，添加对该 JAR 文件的 `compileOnly` 依赖。这表示该 JAR 仅在编译时需要，运行时系统会提供实际的实现。

    参考 `app/build.gradle.kts` 中的配置：

    ```kotlin
    dependencies {
        // ... 其他依赖
        compileOnly(files("libs/ai-api.jar"))
    }
    ```

**重要提示**：使用 `compileOnly` 意味着您期望运行应用程序的 Android 系统环境中已经包含了 `AiManager` 服务的实现。此 JAR 文件本身不包含服务的完整功能，仅用于编译链接。

## 集成

### 1. 获取 AiManager 实例

应用程序需要通过 `Context` 的 `getSystemService` 方法获取 `AiManager` 的实例。请确保您的应用有权访问此系统服务。

```kotlin
import android.ai.kit.AiManager
import android.content.Context

// 在 Activity 或 Service 中
val aiManager = context.getSystemService("ai") as? AiManager

if (aiManager == null) {
    // 处理无法获取服务的情况
    Log.e(TAG, "无法获取 AiManager 服务")
    return
}
```

### 2. 监听结果

要接收AI服务结果，需要实现 `AiCallback` 接口，并将其注册到 `AiManager`。

#### 2.1 实现 AiCallback

接口定义如下 (`onCommandWordRecognized` 已提供默认实现，可选择性覆盖)：

```java
package android.ai.kit;

import androidx.annotation.NonNull;
import java.util.concurrent.Executor;

/**
 * Callback interface for AI service results.
 */
public interface AiCallback {

    /**
     * Called when a command word is recognized.
     *
     * @param commandWord The recognized command word.
     */
     default void onCommandWordRecognized(@NonNull String commandWord){}
}
```

使用方法如下：

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

#### 2.2 注册和取消注册监听器

在需要开始监听时注册回调，在不需要时（例如 Activity 销毁时）取消注册，以避免内存泄漏。

接口定义如下：

```java
/**
 * Adds a listener to receive AI results on the main application thread.
 *
 * @param callback The callback interface implementation. Must not be null.
 * @return An integer result code from the service.
 * @throws NullPointerException     if callback is null.
 * @throws IllegalArgumentException if the listener is already registered.
 * @throws RuntimeException         if the underlying service call fails.
 * @see #addListener(Executor, AiCallback)
 */
public int addListener(@NonNull AiCallback callback)

/**
 * Removes a previously registered listener.
 *
 * @param callback The callback interface implementation to remove. Must not be null.
 * @return An integer result code from the service, or a default value if not found/error.
 * @throws NullPointerException if callback is null.
 * @throws RuntimeException     if the underlying service call fails.
 */
public int removeListener(@NonNull AiCallback callback)
```

使用方法如下：

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

### 3. 离线命令词识别

API 使用 FSA (Finite State Automaton) 格式来定义可识别的命令词语法。

#### 3.0 离线命令词FSA文件书写规范

**FSA文件的核心概念详解**

FSA文件设计用于描述一个有限状态接受器（FSA）网络的结构，该结构由起始节点及多个终止节点构成的图表达。每个规范化的FSA文件都精确对应一个特定的FSA网络。在该网络中，每一条从起始节点通向终止节点的路径代表一条命令路径。网络内所有节点之间的连接均被赋予一个命名，称为"槽"，包括实槽与虚槽两种类型。实槽表示命令路径必须经过的连接，而虚槽则允许命令路径选择跳过。值得注意的是，槽并非命令词的组成部分，而是命令路径的抽象表示，槽的命名不必与命令词直接相关，提供了命名的灵活性。

**FSA文件的格式规范与示例**

FSA文件结构分为两大部分：网络结构信息和槽内命令词信息。网络结构信息涉及节点之间的连接及槽命名，而槽内命令词信息详细描述了每个槽所包含的具体命令词。

* **网络结构信息编写规则**：信息按三列排列，分别代表线段（槽）的起始节点、终止节点和槽名，列之间以Tab键分隔。槽名需用尖括号 `<>` 括起，只能由数字和字母组成，不包含中文。虚槽以 `-` 符号标示，如 `0 1 -` 表示 `<a>` 槽为虚槽，其他为实槽。
* **槽内命令词信息编写规则**：以 `<槽名>:内容;` 格式指定槽内容，内容包括命令词或其部分，同一槽内的不同内容用 `|` 分隔，命令词可以包含中文，但不能包含阿拉伯数字。

**示例说明**：

考虑以下网络结构信息示例：

```fsa
0 1 <a>
0 1 -
1 2 <b>
2 3 <c>
```

与槽内命令词信息示例：

```fsa
<a>:帮我;
<b>:打开;
<c>:电灯|空调;
```

根据上述示例，FSA文件描述了一个简单的命令执行网络。网络中的路径和槽的组合表达了如下命令词：

* 打开电灯
* 帮我打开电灯
* 打开空调
* 帮我打开空调

在这个示例中，虚槽 `0 1 -` 允许命令词路径跳过 `<a>` 槽的"帮我"部分，直接从 `<b>` 槽的"打开"到 `<c>` 槽的"电灯"或"空调"，实现了命令词的灵活组合。这个例子清晰地展示了FSA文件如何通过网络结构和槽内命令词的定义，来指导命令词的生成和理解过程。

参考示例：

```fsa
1  # FSA 1.0;
2  0 1 <a>
3  0 1 -
4  1 2 <b>
5  2 3 <c>
6  ;
7  <a>:今天;
8  <b>:下雨;
9  <c>:了吗|下没;
```

**示例解析**：

FSA 文件的结构是固定的，主要包含以下几个部分：

* **第 1 行 (`# FSA 1.0;`)**: 文件头。这是一个固定格式的标识符，表明这是一个 FSA 1.0 版本的文件，必须存在。
* **第 2-5 行 (网络结构定义)**: 定义了状态节点之间的转换（边）以及对应的槽。
  * 格式为 `起始节点号<Tab>终止节点号<Tab><槽名或->`。
  * `0 1 <a>` 表示从节点 0 到节点 1 有一条边，这条边对应名为 `<a>` 的实槽。
  * `0 1 -` 表示从节点 0 到节点 1 还有一条边，这条边是虚槽（用 `-` 表示），意味着路径可以选择跳过对应的实槽（此处是 `<a>`）。
  * 这部分的行数取决于网络结构的复杂程度。
* **第 6 行 (`;`)**: 分隔符。这是一个固定的单分号行，用于分隔网络结构定义和槽内命令词定义，必须存在。
* **第 7-9 行 (槽内命令词定义)**: 定义了每个槽（Slot）具体可以匹配哪些词语。
  * 格式为 `<槽名>:<命令词1|命令词2|...>;`。
  * `<a>:今天;` 定义了槽 `<a>` 包含命令词"今天"。
  * `<c>:了吗|下没;` 定义了槽 `<c>` 包含命令词"了吗"或者"下没"（用 `|` 分隔）。
  * 每个槽定义以分号 `;` 结尾。
  * 这部分的行数取决于定义了多少个槽。

总的来说，文件头、分隔符是固定存在的。网络结构定义和槽内命令词定义的**格式**是固定的，但具体的**行数和内容**会根据你定义的命令词语法网络而变化。

#### 3.1 设置命令词

使用 `setCommandWords` 方法可以添加或更新一个特定场景（由 `key` 标识）的命令词语法。

方法定义如下：

```java
/**
 * Sets the command words FSA content for a given key and language type.
 *
 * @param key          The key associated with the command words. Must not be null.
 * @param fsaContent   The FSA content string. Must not be null.
 * @param languageType The language type. Must be one of {@link AiConstants#LANGUAGE_TYPE_CHINESE}
 *                     or {@link AiConstants#LANGUAGE_TYPE_ENGLISH}.
 * @return An integer result code from the service.
 * @throws NullPointerException     if key or fsaContent is null.
 * @throws IllegalArgumentException if languageType is invalid.
 * @throws RuntimeException         if the underlying service call fails.
 */
public int setCommandWords(@NonNull String key, @NonNull String fsaContent, @AiConstants.LanguageType int languageType)
```

```kotlin
import android.ai.kit.AiConstants

val key = "key" // 自定义场景 Key，简单标准字符串即可
val commandWordFsaContent = "#FSA 1.0;\n" +
        "0\t1\t<a>\n" +
        "0\t1\t-\n" +
        "1\t2\t<b>\n" +
        "2\t3\t<c>\n" +
        ";\n" +
        "<a>:今天;\n" +
        "<b>:你好;\n" +
        "<c>:了吗|不好;"

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

#### 3.2 获取指定语言的所有命令词 FSA 内容

使用 `getAllCommandWordFsaContent` 可以获取当前已为指定语言设置的所有场景的 FSA 内容集合，返回一个以
`key` 为键，FSA 内容为值的 Map。

方法定义如下：

```java
/**
 * Retrieves all command word FSA content for a given language type.
 *
 * @param languageType The language type. Must be one of {@link AiConstants#LANGUAGE_TYPE_CHINESE}
 *                     or {@link AiConstants#LANGUAGE_TYPE_ENGLISH}.
 * @return A Map where keys are command word keys and values are FSA content strings.
 * Returns an empty map if no content is found or in case of error.
 * @throws IllegalArgumentException if languageType is invalid.
 * @throws RuntimeException         if the underlying service call fails.
 */
public Map<String, String> getAllCommandWordFsaContent(@AiConstants.LanguageType int languageType)
```

```kotlin
val allFsaContent: Map<String, String>? =
    aiManager?.getAllCommandWordFsaContent(AiConstants.LANGUAGE_TYPE_CHINESE)

if (allFsaContent != null) {
    for ((key, fsa) in allFsaContent) { // 迭代 key 和 fsa
        Log.d(TAG, "场景 Key [$key]:\n$fsa")
    }
} else {
    Log.e(TAG, "获取所有命令词失败")
}
```

#### 3.3 删除指定场景的命令词

使用 `deleteCommandWordFsaContent` 可以删除指定 `key` 和 `languageType` 的命令词语法。
**如果 `key` 为空字符串 `""`，则会删除指定 `languageType` 下的所有命令词语法。**

方法定义如下：

```java
/**
 * Deletes the command word FSA content associated with the given key and language type.
 *
 * @param key          The key of the command word FSA content to delete. Must not be null but can be empty.
 *                     If empty, all command word FSA content for the given language type will be deleted.
 * @param languageType The language type. Must be one of {@link AiConstants#LANGUAGE_TYPE_CHINESE}
 *                     or {@link AiConstants#LANGUAGE_TYPE_ENGLISH}.
 * @return An integer result code from the service.
 * @throws NullPointerException     if key is null.
 * @throws IllegalArgumentException if languageType is invalid.
 * @throws RuntimeException         if the underlying service call fails.
 */
public int deleteCommandWordFsaContent(@NonNull String key, @AiConstants.LanguageType int languageType)
```

```kotlin
val keyToDelete = "" // 删除中文下的所有命令词
val deleteResultCode: Int? = aiManager?.deleteCommandWordFsaContent(keyToDelete, AiConstants.LANGUAGE_TYPE_CHINESE)
if (deleteResultCode == 0) {
    Log.d(TAG, "删除所有中文命令词成功")
} else {
    Log.e(TAG, "删除所有中文命令词失败，错误码: $deleteResultCode")
}
```

### 4. 常量说明

API 使用 `AiConstants` 类来定义常量。

* `AiConstants.LANGUAGE_TYPE_CHINESE`: 表示中文。
* `AiConstants.LANGUAGE_TYPE_ENGLISH`: 表示英文。

请根据需要选择合适的语言类型。

## 完整示例

一个完整的演示如何在 Activity 中使用 `AiManager` 的示例代码，请参考项目中的以下文件：

[app/src/main/java/com/ai/kit/example/MainActivity.kt](app/src/main/java/com/ai/kit/example/MainActivity.kt)

该文件展示了如何：

* 获取 `AiManager` 实例。
* 实现 `AiCallback` 接口并处理识别结果。
* 在 Activity 生命周期中注册和取消注册监听器。
* 使用按钮触发设置、获取和删除命令词的操作。
