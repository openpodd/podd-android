# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Android Studio.app/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontwarn org.mozilla.javascript.tools.**
-dontwarn org.mozilla.javascript.xml.impl.**
-dontwarn org.apache.xmlbeans.XmlCursor
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.** { *; }

-dontwarn org.apache.http.**
-dontwarn org.apache.james.mime4j.**
-dontwarn org.apache.commons.logging.LogFactory

# funf
-keep class edu.mit.media.funf.** { *; }

# httpcore
-keep class org.apache.http.** { *; }
-dontwarn javax.swing.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**

# amazon
-keep class org.apache.commons.logging.**               { *; }
-keep class com.amazonaws.services.sqs.QueueUrlHandler  { *; }
-keep class com.amazonaws.javax.xml.transform.sax.*     { public *; }
-keep class com.amazonaws.javax.xml.stream.**           { *; }
-keep class com.amazonaws.services.**.model.*Exception* { *; }
-keep class com.amazonaws.internal.**                   { *; }
-keep class org.codehaus.**                             { *; }
-keep class org.joda.time.tz.Provider                    { *; }
-keep class org.joda.time.tz.NameProvider                { *; }
-keep class com.amazonaws.org.joda.convert.**          { *; }
-keep class com.amazonaws.org.apache.commons.logging.**    { *; }
-keepattributes Signature,*Annotation*,EnclosingMethod
-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class com.amazonaws.** { *; }

-dontwarn com.amazonaws.org.joda.time.**
-dontwarn org.apache.http.conn.scheme.**
-dontwarn com.fasterxml.jackson.databind.**
-dontwarn javax.xml.stream.events.**
-dontwarn org.codehaus.jackson.**
-dontwarn org.apache.commons.logging.impl.**
-dontwarn org.apache.http.conn.scheme.**
-dontwarn org.apache.http.annotation.**
-dontwarn org.ietf.jgss.**
-dontwarn org.joda.convert.**
-dontwarn org.w3c.dom.bootstrap.**
-dontwarn com.squareup.okhttp.**
-dontwarn okio.**

-dontwarn com.amazonaws.services.s3.**

-dontnote com.amazonaws.services.sqs.QueueUrlHandler

# support-v4
-dontwarn android.support.v4.**
-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }

# support-v7
-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep class android.support.v7.internal.** { *; }
-keep interface android.support.v7.internal.** { *; }

-keep class org.cm.podd.report.** { *; }

# google analytics
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# play
-keep public class com.google.android.gms.* { public *; }
-dontwarn com.google.android.gms.**

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# firebase
-keepclassmembernames class org.cm.podd.report.model.RecordData {
    *;
}

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception