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

import kotlin.test.Test
import kotlin.test.assertEquals

class NativeReadTest {

    @Test
    fun testNativeRead() {
        with(NativeRead()) {
            size = 8
            setup()
            val expectedValue = 0 + 1 + 2 + 3 + 4 + 5 + 6 + 7
            assertEquals(expectedValue, readByteArray())
            assertEquals(expectedValue, readHeapBuffer())
            assertEquals(expectedValue, readHeapBuffer()) // test rewind
            assertEquals(expectedValue, readDirectBuffer())
            assertEquals(expectedValue, readDirectBuffer()) // test rewind
            assertEquals(expectedValue, readSegmentGlobal())
            assertEquals(expectedValue, readSegmentAuto())
            assertEquals(expectedValue, readSegmentConfined())
            assertEquals(expectedValue, readSegmentShared())
        }
    }
}