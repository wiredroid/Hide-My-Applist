import com.android.ide.common.signing.KeystoreHelper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.PrintStream
import java.util.*

plugins {
    kotlin("android")
    id("com.android.application")
    id("com.google.gms.google-services")
    id("dev.rikka.tools.materialthemebuilder")
    id("dev.rikka.tools.refine")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.tsng.hidemyapplist"

    buildFeatures {
        viewBinding = true
    }
}

fun afterEval() = android.applicationVariants.forEach { variant ->
    val variantCapped = variant.name.capitalize(Locale.ROOT)
    val variantLowered = variant.name.toLowerCase(Locale.ROOT)

    val outSrcDir = file("$buildDir/generated/source/signInfo/${variantLowered}")
    val outSrc = file("$outSrcDir/com/tsng/hidemyapplist/Magic.java")
    val signInfoTask = task("generate${variantCapped}SignInfo") {
        dependsOn("validateSigning${variantCapped}")
        outputs.file(outSrc)
        doLast {
            val sign = android.buildTypes[variantLowered].signingConfig
            outSrc.parentFile.mkdirs()
            val certificateInfo = KeystoreHelper.getCertificateInfo(
                sign?.storeType,
                sign?.storeFile,
                sign?.storePassword,
                sign?.keyPassword,
                sign?.keyAlias
            )
            PrintStream(outSrc).apply {
                println("package com.tsng.hidemyapplist;")
                println("public final class Magic {")
                print("public static final byte[] magicNumbers = {")
                val bytes = certificateInfo.certificate.encoded
                print(bytes.joinToString(",") { it.toString() })
                println("};")
                println("}")
            }
        }
    }
    variant.registerJavaGeneratingTask(signInfoTask, arrayListOf(outSrcDir))

    val kotlinCompileTask = tasks.findByName("compile${variantCapped}Kotlin") as KotlinCompile
    kotlinCompileTask.dependsOn(signInfoTask)
    val srcSet = objects.sourceDirectorySet("magic", "magic").srcDir(outSrcDir)
    kotlinCompileTask.source(srcSet)

    task<Sync>("build$variantCapped") {
        dependsOn("assemble$variantCapped")
        from("$buildDir/outputs/apk/$variantLowered")
        into("$buildDir/apk/$variantLowered")
        rename(".*.apk", "HMA-V${variant.versionName}-${variant.buildType.name}.apk")
    }
}

afterEvaluate {
    afterEval()
}

dependencies {
    implementation(projects.common)
    runtimeOnly(projects.xposed)

    implementation("androidx.navigation:navigation-fragment-ktx:2.5.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.1")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.drakeet.about:about:2.5.1")
    implementation("com.drakeet.multitype:multitype:4.3.0")
    implementation("com.github.kirich1409:viewbindingpropertydelegate:1.5.6")
    implementation("com.github.topjohnwu.libsu:core:3.1.2")
    implementation("com.google.android.material:material:1.6.1")
    implementation("com.google.android.gms:play-services-ads:21.1.0")
    implementation("com.google.firebase:firebase-analytics-ktx:21.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("dev.rikka.hidden:compat:3.2.0")
    implementation("dev.rikka.rikkax.material:material:2.5.1")
    implementation("dev.rikka.rikkax.material:material-preference:2.0.0")
    implementation("me.zhanghai.android.appiconloader:appiconloader:1.4.0")
    compileOnly("dev.rikka.hidden:stub:3.2.0")
}

configurations.all {
    exclude("androidx.appcompat", "appcompat")
}
