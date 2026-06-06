package com.nihaltp.smartringtone.data

import android.content.Context
import android.net.Uri

object GitHubIssueHelper {
    fun getAppVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getAppVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun getInstallSource(context: Context): String {
        val installerPackage =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
                } catch (e: Exception) {
                    null
                }
            } else {
                try {
                    @Suppress("DEPRECATION")
                    context.packageManager.getInstallerPackageName(context.packageName)
                } catch (e: Exception) {
                    null
                }
            }

        return when (installerPackage) {
            "org.fdroid.fdroid" -> "F-Droid"
            "com.looker.droidify" -> "Droid-ify"
            "com.android.vending" -> "Google Play Store"
            "com.amazon.venezia" -> "Amazon Appstore"
            null, "", "com.google.android.packageinstaller", "com.android.packageinstaller" -> "Sideload / ADB / Local Installer"
            else -> installerPackage
        }
    }

    fun getDownloadSource(context: Context): String {
        val downloaderPackage =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    context.packageManager.getInstallSourceInfo(context.packageName).originatingPackageName
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

        return when (downloaderPackage) {
            "org.fdroid.fdroid" -> "F-Droid"
            "com.looker.droidify" -> "Droid-ify"
            "com.android.vending" -> "Google Play Store"
            "com.amazon.venezia" -> "Amazon Appstore"
            null, "", "com.google.android.packageinstaller", "com.android.packageinstaller" -> "Sideload / ADB / Local Installer"
            else -> downloaderPackage
        }
    }

    fun getReportUrl(
        context: Context,
        throwable: Throwable,
    ): String {
        val stackTrace = android.util.Log.getStackTraceString(throwable)
        val versionName = getAppVersionName(context)
        val versionCode = getAppVersionCode(context)
        val installSource = getInstallSource(context)
        val downloadSource = getDownloadSource(context)

        val exceptionName = throwable.javaClass.simpleName.ifEmpty { throwable.javaClass.name }
        val errorMessage = throwable.message?.take(60)?.replace('\n', ' ') ?: "Unknown Error"
        val title = "[Bug] $exceptionName: $errorMessage"

        val body =
            """
            ### ⚠️ Error Description
            ${throwable.localizedMessage ?: throwable.message ?: "No error message provided."}

            ### 📱 Device & Environment Info
            - **Device:** ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
            - **Android Version:** ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})
            - **App Version Name:** $versionName
            - **App Version Code:** $versionCode
            - **Installation Source:** $installSource
            - **Download Source:** $downloadSource

            ### 🔍 Stack Trace
            ```kotlin
            $stackTrace
            ```
            """.trimIndent()

        return "https://github.com/nihaltp/SmartRingtone/issues/new" +
            "?title=${Uri.encode(title)}" +
            "&body=${Uri.encode(body)}"
    }
}
