#!/usr/bin/env bash

## See instructions at https://github.com/google/ExoPlayer/tree/release-v2/extensions/flac

# Set environment variables.
EXOPLAYER_ROOT="$(pwd)/ExoPlayer"
FLAC_EXT_PATH="${EXOPLAYER_ROOT}/extensions/flac/src/main"
NDK_PATH="${ANDROID_HOME}/android-ndk-r15c"    # Needs NDK version <= 17c

# Download and extract flac-1.3.2 as "${FLAC_EXT_PATH}/jni/flac" folder:
cd "${FLAC_EXT_PATH}/jni" && \
curl https://ftp.osuosl.org/pub/xiph/releases/flac/flac-1.3.2.tar.xz | tar xJ && \
mv flac-1.3.2 flac

# Build the JNI native libraries from the command line:
cd "${FLAC_EXT_PATH}"/jni && \
${NDK_PATH}/ndk-build APP_ABI=all -j4