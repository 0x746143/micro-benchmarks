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

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeWriteTest {

    @Test
    fun testNativeWrite() {
        with(NativeWrite()) {
            size = 8
            setup()
            validateByteArray(writeByteArray())
            validateHeapBuffer(writeHeapBuffer())
            validateHeapBuffer(writeHeapBuffer()) // test rewind
            validateHeapBuffer(writeDirectBuffer())
            validateHeapBuffer(writeDirectBuffer()) // test rewind
            validateMemorySegment(writeSegmentGlobal())
            validateMemorySegment(writeSegmentAuto())
            validateMemorySegment(writeSegmentConfined())
            validateMemorySegment(writeSegmentShared())
        }
    }

    fun validateByteArray(byteArray: ByteArray) {
        for (i in 0 until byteArray.size) {
            assertEquals(i.toByte(), byteArray[i])
        }
    }

    fun validateHeapBuffer(byteBuffer: ByteBuffer) {
        for (i in 0 until byteBuffer.limit()) {
            assertEquals(i.toByte(), byteBuffer.get(i))
        }
    }

    fun validateMemorySegment(memorySegment: MemorySegment) {
        for (i in 0 until memorySegment.byteSize()) {
            assertEquals(i.toByte(), memorySegment.get(JAVA_BYTE, i))
        }
    }
}