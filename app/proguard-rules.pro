-dontobfuscate

# Shizuku UserService (instantiated by Shizuku in a separate process by name)
-keep class io.github.jqssun.displayextend.shizuku.UserService { *; }
-keep class io.github.jqssun.displayextend.shizuku.IUserService { *; }
-keep class io.github.jqssun.displayextend.shizuku.IUserService$Stub { *; }

# AIDL generated
-keep class * implements android.os.IInterface { *; }

-keep class io.github.jqssun.displayextend.** extends android.os.Binder { *; }

# Shizuku
-keep class rikka.shizuku.** { *; }
