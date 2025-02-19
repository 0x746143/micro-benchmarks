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

import org.junit.jupiter.api.Test
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.nio.ByteBuffer
import kotlin.test.assertEquals

class NativeCallTest {

    @Test
    fun testSystemCallGetPid() = with(NativeCall()) {
        val expectedPid = ProcessHandle.current().pid().toInt()
        assertEquals(expectedPid, systemCallGetPidJni())
        assertEquals(expectedPid, systemCallGetPidFfm())
        assertEquals(expectedPid, systemCallGetPidFfmExact())
    }

    @Test
    fun testMemcpy() = with(NativeCall()) {
        testMemcpy(::memcpyJniByObject, srcBuffer, dstBuffer)
        testMemcpy(::memcpyJniByAddress, srcBuffer, dstBuffer)
        testMemcpy(::memcpyJniByAddressUnsafe, srcBuffer, dstBuffer)
        testMemcpy(::memcpyFfmGlobalInvoke, srcSegmentGlobal, dstSegmentGlobal)
        testMemcpy(::memcpyFfmGlobalInvokeExact, srcSegmentGlobal, dstSegmentGlobal)
        testMemcpy(::memcpyFfmAutoInvoke, srcSegmentAuto, dstSegmentAuto)
        testMemcpy(::memcpyFfmAutoInvokeExact, srcSegmentAuto, dstSegmentAuto)
        testMemcpy(::memcpyFfmSharedInvoke, srcSegmentShared, dstSegmentShared)
        testMemcpy(::memcpyFfmSharedInvokeExact, srcSegmentShared, dstSegmentShared)
        testMemcpy(::memcpyFfmConfinedInvoke, srcSegmentConfined, dstSegmentConfined)
        testMemcpy(::memcpyFfmConfinedInvokeExact, srcSegmentConfined, dstSegmentConfined)
    }

    private fun testMemcpy(memcpyFunc: () -> Unit, src: ByteBuffer, dst: ByteBuffer) {
        src.clear().putLong(0x1234567812345678)
        dst.clear().putLong(0).flip()
        memcpyFunc()
        assertEquals(0x1234567812345678, dst.getLong())
    }

    private fun testMemcpy(memcpyFunc: () -> Unit, src: MemorySegment, dst: MemorySegment) {
        src.set(JAVA_LONG, 0, 0x1234567812345678)
        dst.set(JAVA_LONG, 0, 0)
        memcpyFunc()
        assertEquals(0x1234567812345678, dst.get(JAVA_LONG, 0))
    }
}