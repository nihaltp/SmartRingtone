# Keep generic signature attributes to prevent TypeToken erasure
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Keep the TypeToken class and all its subclasses to prevent type erasure on Gson TypeToken anonymous classes
-keep class * extends com.google.gson.reflect.TypeToken

# Keep the model classes that are serialized/deserialized with Gson
-keep class com.nihaltp.smartringtone.data.Ringtone { *; }
-keep class com.nihaltp.smartringtone.data.CallLogEntry { *; }
-keep class com.nihaltp.smartringtone.data.Contact { *; }
-keep class com.nihaltp.smartringtone.data.ContactSortOrder { *; }
