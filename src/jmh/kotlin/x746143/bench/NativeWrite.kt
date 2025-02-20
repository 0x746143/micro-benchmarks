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
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.*
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

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
class NativeWrite {
    private val jniAdapter = JniAdapter()
    private val unsafe = getUnsafe()
    private val fieldOffset = Buffer::class.getFieldOffset("address", unsafe)
    private val addressHandle = Buffer::class.getVarHandle("address", Long::class.javaPrimitiveType)
    private val memcpy = getNativeFuncHandle("memcpy", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT))

    @Param("4", "16", "256", "65536")
    var size = 0
    lateinit var byteArray: ByteArray
    lateinit var heapBuffer: ByteBuffer
    lateinit var directBuffer: ByteBuffer
    private val arenaGlobal = Arena.global()
    private val arenaAuto = Arena.ofAuto()
    private val arenaConfined = Arena.ofConfined()
    private val arenaShared = Arena.ofShared()
    lateinit var segmentGlobal: MemorySegment
    lateinit var segmentAuto: MemorySegment
    lateinit var segmentConfined: MemorySegment
    lateinit var segmentShared: MemorySegment

    @Setup
    fun setup() {
        val lsize = size.toLong()
        byteArray = ByteArray(size)
        heapBuffer = ByteBuffer.allocate(size)!!
        directBuffer = ByteBuffer.allocateDirect(size)!!
        segmentGlobal = arenaGlobal.allocate(lsize)!!
        segmentAuto = arenaAuto.allocate(lsize)!!
        segmentConfined = arenaConfined.allocate(lsize)!!
        segmentShared = arenaShared.allocate(lsize)!!
        for (i in 0 until size) {
            val value = i.toByte()
            val offset = i.toLong()
            byteArray[i] = value
            heapBuffer.put(value)
            directBuffer.put(value)
            segmentGlobal.set(JAVA_BYTE, offset, value)
            segmentAuto.set(JAVA_BYTE, offset, value)
            segmentConfined.set(JAVA_BYTE, offset, value)
            segmentShared.set(JAVA_BYTE, offset, value)
        }
        heapBuffer.flip()
        directBuffer.flip()
    }

    @Benchmark
    fun writeByteArray(): ByteArray {
        for (i in 0 until size) {
            byteArray[i] = i.toByte()
        }
        return byteArray
    }

    @Benchmark
    fun writeHeapBuffer(): ByteBuffer {
        for (i in 0 until size) {
            heapBuffer.put(i.toByte())
        }
        return heapBuffer.rewind()
    }

    @Benchmark
    fun writeDirectBuffer(): ByteBuffer {
        for (i in 0 until size) {
            directBuffer.put(i.toByte())
        }
        return directBuffer.rewind()
    }

    @Benchmark
    fun writeSegmentGlobal(): MemorySegment {
        for (i in 0 until size.toLong()) {
            segmentGlobal.set(JAVA_BYTE, i, i.toByte())
        }
        return segmentGlobal
    }

    @Benchmark
    fun writeSegmentAuto(): MemorySegment {
        for (i in 0 until size.toLong()) {
            segmentAuto.set(JAVA_BYTE, i, i.toByte())
        }
        return segmentAuto
    }

    @Benchmark
    fun writeSegmentConfined(): MemorySegment {
        for (i in 0 until size.toLong()) {
            segmentConfined.set(JAVA_BYTE, i, i.toByte())
        }
        return segmentConfined
    }

    @Benchmark
    fun writeSegmentShared(): MemorySegment {
        for (i in 0 until size.toLong()) {
            segmentShared.set(JAVA_BYTE, i, i.toByte())
        }
        return segmentShared
    }
}