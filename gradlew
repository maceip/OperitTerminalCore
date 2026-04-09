#!/bin/sh
# Gradle wrapper bootstrap — downloads the wrapper jar if missing, then runs it.
# For a full wrapper, run: gradle wrapper --gradle-version 8.11.1
set -e

APP_NAME="Gradle"
CLASSPATH="gradle/wrapper/gradle-wrapper.jar"
GRADLE_URL="https://services.gradle.org/distributions/gradle-8.11.1-bin.zip"

if [ ! -f "$CLASSPATH" ]; then
    echo "Downloading Gradle wrapper jar..."
    mkdir -p gradle/wrapper
    curl -fsSL -o gradle/wrapper/gradle-wrapper.jar \
        "https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar"
fi

# Determine the Java command
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" \
    -Xmx2048m \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
