# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---- AI Kit (android.ai.kit) ----
# 1) 保留 AI Kit 包下的所有类/接口及其成员，防止 R8 混淆/裁剪导致运行时缺类
# 2) 特别保留接口默认方法相关的合成类（如 $-CC 等），避免 NoClassDefFoundError
# 3) 保留必要的属性，确保反射/注解/内部类信息在运行时可用
-keep class android.ai.kit.** { *                                                                                                                                                                      ; }
-keep interface android.ai.kit.** { *                                                                                                                                                                  ; }
-keep class android.ai.kit.**$* { *                                                                                                                                                                    ; }
-keepattributes InnerClasses,EnclosingMethod,Signature,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,AnnotationDefault
-dontwarn android.ai.kit.**
