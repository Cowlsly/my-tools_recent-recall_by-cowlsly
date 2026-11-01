plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// The three original PNGs stay untouched in /res/drawable at the repo root.
// This task copies only the PNGs into a generated resource folder so the app can use them at runtime.
val copyRepoDrawables by tasks.registering(Copy::class) {
    from(rootProject.layout.projectDirectory.dir("res/drawable")) { include("*.png") }
    into(layout.buildDirectory.dir("generated/repoRes/drawable"))
}

android {
    namespace = "com.cowslylabs.recentrecall"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cowslylabs.recentrecall"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-debug"
    }

    sourceSets.getByName("main").res.srcDir(layout.buildDirectory.dir("generated/repoRes"))

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = false
    }
}

tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(copyRepoDrawables) }
// Lint and resource tasks read the generated folder, so run the copy first.
tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("Resources") || name.startsWith("lint") || name.startsWith("generate") && name.endsWith("Resources")) {
        dependsOn(copyRepoDrawables)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
