[Project Page](https://honeyroasted.github.io/jype/landing.html)

Still heavily WIP. Currently missing type inference functionality.

# Jype
Jype is a library providing a set of classes fully representing Java's type system, including its
primitives, generics, and more. It is capable of generating signatures for these types, as well as 
testing assignability between types.

## Building
Jype may be built using jitpack:  
[![Release](https://jitpack.io/v/HoneyRoasted/Jype.svg)](https://jitpack.io/#HoneyRoasted/Jype)

```groovy
repositories {
    maven {url 'https://jitpack.io'}    
}

dependencies {
    implementation 'com.github.HoneyRoasted:Jype:Version'
}
```
Additionally, Jype is continuously built with [GitHub actions](https://github.com/HoneyRoasted/Jype/actions). You
may also download the repository and build from source using Gradle or the `build.sh` script.