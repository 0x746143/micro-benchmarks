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
package x746143.bench

import org.openjdk.jmh.annotations.*
import sun.misc.Unsafe
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import java.lang.reflect.Field
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

@Suppress("unused")
@Threads(1)
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 4, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(
    4, jvmArgs = [
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens", "java.base/java.nio=ALL-UNNAMED",
        "-Djava.library.path=build/native"
    ]
)
class NativeCall {
    private val jniAdapter = JniAdapter()
    private val unsafe = getUnsafe()
    private val fieldOffset = Buffer::class.getFieldOffset("address")
    private val addressHandle = Buffer::class.getVarHandle("address", Long::class.javaPrimitiveType)

    private val getpid = getNativeFuncHandle("getpid", FunctionDescriptor.of(JAVA_INT))
    private val memcpy = getNativeFuncHandle("memcpy", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT))

    private val size = 8
    private val lsize = size.toLong()
    private var srcBuffer = ByteBuffer.allocateDirect(size)
    private var dstBuffer = ByteBuffer.allocateDirect(size)

    private var arenaGlobal = Arena.global()
    private var arenaAuto = Arena.ofAuto()
    private var arenaShared = Arena.ofShared()
    private var arenaConfined = Arena.ofConfined()
    private var srcSegmentGlobal = arenaGlobal.allocate(lsize)
    private var dstSegmentGlobal = arenaGlobal.allocate(lsize)
    private var srcSegmentAuto = arenaAuto.allocate(lsize)
    private var dstSegmentAuto = arenaAuto.allocate(lsize)
    private var srcSegmentShared = arenaShared.allocate(lsize)
    private var dstSegmentShared = arenaShared.allocate(lsize)
    private var srcSegmentConfined = arenaConfined.allocate(lsize)
    private var dstSegmentConfined = arenaConfined.allocate(lsize)

    @Benchmark
    fun systemCallGetPidJni(): Int {
        return jniAdapter.getPid()
    }

    @Benchmark
    fun systemCallGetPidFfm(): Int {
        return getpid.invoke() as Int
    }

    @Benchmark
    fun systemCallGetPidFfmExact(): Int {
        return getpid.invokeExact() as Int
    }

    @Benchmark
    fun memcpyJniByObject(): ByteBuffer {
        jniAdapter.memcpyByObject(dstBuffer, srcBuffer, size)
        return dstBuffer
    }

    @Benchmark
    fun memcpyJniByAddress(): ByteBuffer {
        val dstAddress = addressHandle.get(dstBuffer) as Long
        val srcAddress = addressHandle.get(srcBuffer) as Long
        jniAdapter.memcpyByAddress(dstAddress, srcAddress, size)
        return dstBuffer
    }

    @Benchmark
    fun memcpyJniByAddressUnsafe(): ByteBuffer {
        val dstAddress = unsafe.getLong(dstBuffer, fieldOffset)
        val srcAddress = unsafe.getLong(srcBuffer, fieldOffset)
        jniAdapter.memcpyByAddress(dstAddress, srcAddress, size)
        return dstBuffer
    }

    @Benchmark
    fun memcpyFfmGlobalInvoke(): MemorySegment {
        memcpy.invoke(dstSegmentGlobal, srcSegmentGlobal, size)
        return dstSegmentGlobal
    }

    @Benchmark
    fun memcpyFfmGlobalInvokeExact(): MemorySegment {
        memcpy.invokeExact(dstSegmentGlobal, srcSegmentGlobal, size)
        return dstSegmentGlobal
    }

    @Benchmark
    fun memcpyFfmAutoInvoke(): MemorySegment {
        memcpy.invoke(dstSegmentAuto, srcSegmentAuto, size)
        return dstSegmentAuto
    }

    @Benchmark
    fun memcpyFfmAutoInvokeExact(): MemorySegment {
        memcpy.invokeExact(dstSegmentAuto, srcSegmentAuto, size)
        return dstSegmentAuto
    }

    @Benchmark
    fun memcpyFfmSharedInvoke(): MemorySegment {
        memcpy.invoke(dstSegmentShared, srcSegmentShared, size)
        return dstSegmentShared
    }

    @Benchmark
    fun memcpyFfmSharedInvokeExact(): MemorySegment {
        memcpy.invokeExact(dstSegmentShared, srcSegmentShared, size)
        return dstSegmentShared
    }

    @Benchmark
    fun memcpyFfmConfinedInvoke(): MemorySegment {
        memcpy.invoke(dstSegmentConfined, srcSegmentConfined, size)
        return dstSegmentConfined
    }

    @Benchmark
    fun memcpyFfmConfinedInvokeExact(): MemorySegment {
        memcpy.invokeExact(dstSegmentConfined, srcSegmentConfined, size)
        return dstSegmentConfined
    }

    private fun getUnsafe(): Unsafe {
        return Unsafe::class.getField("theUnsafe").get(null) as Unsafe
    }

    @Suppress("DEPRECATION")
    private fun KClass<*>.getFieldOffset(name: String): Long {
        return getField(name).let { unsafe.objectFieldOffset(it) }
    }

    private fun KClass<*>.getField(name: String): Field {
        return java.getDeclaredField(name).apply { isAccessible = true }
    }

    private fun KClass<*>.getVarHandle(name: String, type: Class<Long>?): VarHandle {
        return MethodHandles.privateLookupIn(java, MethodHandles.lookup()).findVarHandle(java, name, type)
    }

    private fun getNativeFuncHandle(name: String, descriptor: FunctionDescriptor): MethodHandle {
        val linker = Linker.nativeLinker()
        return linker.defaultLookup().find(name)
            .map { linker.downcallHandle(it, descriptor) }
            .orElseThrow()
    }
}

