/*
 * Copyright 2025 0x746143
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <jni.h>
#include <unistd.h>
#include <string.h>
#include <stdint.h>

JNIEXPORT jlong JNICALL Java_x746143_bench_JniAdapter_getPid(JNIEnv *env, jobject obj) {
    return getpid();
}

JNIEXPORT void JNICALL Java_x746143_bench_JniAdapter_memcpyByObject(JNIEnv *env, jobject obj, jobject dst, jobject src, jint size) {
    void *dstPtr = (*env)->GetDirectBufferAddress(env, dst);
    void *srcPtr = (*env)->GetDirectBufferAddress(env, src);
    memcpy(dstPtr, srcPtr, size);
}

JNIEXPORT void JNICALL Java_x746143_bench_JniAdapter_memcpyByAddress(JNIEnv *env, jobject obj, jlong dstAddress, jlong srcAddress, jint size) {
    memcpy((void *)dstAddress, (void *)srcAddress, size);
}

