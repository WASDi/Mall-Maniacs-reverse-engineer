plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // OSX fix: Download from https://www.lwjgl.org/browse/release/3.3.1/bin into ~/Library/Java/Extensions
    implementation("org.lwjgl:lwjgl:3.3.1")
    implementation("org.lwjgl:lwjgl-opengl:3.3.1")
    implementation("org.lwjgl:lwjgl-glfw:3.3.1")
    implementation("org.joml:joml:1.10.5")

    implementation("org.l33tlabs.twl:pngdecoder:1.0")

    implementation(files("/Users/wasd/repos/wasd-lib3d/app/build/libs/wasd-lib3d.jar"))

    testImplementation("junit:junit:4.13.2")
}
