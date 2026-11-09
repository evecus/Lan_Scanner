# release 已开启 R8（isMinifyEnabled / isShrinkResources）。
# 以下规则为 R8 fullMode 下的必要保留项，其余未引用代码（含
# material-icons-extended 中未使用的图标）由 R8 自动裁剪。

-keepattributes *Annotation*
-dontwarn kotlinx.coroutines.**

# 业务代码保持原样（不混淆、不内联移除），只让 R8 裁剪第三方冗余
# （material-icons-extended 等）。Kotlin object / Compose 生成代码 /
# ViewModel 反射实例化都在此包内，保留可彻底避免运行期回归。
-keep class com.lan.scanner.** { *; }

# kotlinx-coroutines-android 通过 ServiceLoader 反射加载 Dispatchers.Main，
# fullMode 下需 keepnames，否则运行期报 "Module with the Main dispatcher is missing"。
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
