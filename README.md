# blueprint
Blueprint based on files in notebook git repository


```
--illegal-access=warn
-Dio.netty.tryReflectionSetAccessible=true
--add-opens java.base/java.nio=io.netty.common
--add-opens java.base/jdk.internal.misc=io.netty.common
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED
```

# Run unit tests in IntelliJ with the following options
```
--illegal-access=warn
-Dio.netty.tryReflectionSetAccessible=true
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/sun.nio.ch=ALL-UNNAMED
--add-opens java.base/java.io=ALL-UNNAMED
```
