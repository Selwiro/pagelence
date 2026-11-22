# :app-phone 的 R8 规则。
#
# release 已开启混淆与资源压缩（T155）。这里只放 R8 猜不到的东西：
# kotlinx.serialization 通过反射找生成的 serializer，其余（Compose、AndroidX、
# OkHttp）各自带 consumer rules，不需要在这里重复。

# kotlinx.serialization：保留 @Serializable 类生成的 serializer 与 Companion。
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 本项目所有 @Serializable 模型都这样被查找。
-keep,includedescriptorclasses class com.pagelence.**$$serializer { *; }
-keepclassmembers class com.pagelence.** {
    *** Companion;
}
-keepclasseswithmembers class com.pagelence.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 枚举被序列化时按名字写入文档，R8 不得改名（内容与进度都依赖这些名字）。
-keepclassmembers enum com.pagelence.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
