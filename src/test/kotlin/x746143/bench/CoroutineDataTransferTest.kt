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
import org.openjdk.jmh.annotations.Benchmark
import java.nio.ByteBuffer
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation
import kotlin.test.assertEquals

class CoroutineDataTransferTest {

    @Test
    fun testCoroutineDataTransfer() {
        val cdt = CoroutineDataTransfer()
        val expected = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
        cdt.times = expected.length
        cdt::class.functions
            .filter { it.hasAnnotation<Benchmark>() }
            .filter { it.name != "jvmOptimizationVerifier" }
            .forEach {
                val actual = with(it.call(cdt) as ByteBuffer) { String(array(), 0, position()) }
                assertEquals(expected, actual, it.name)
            }
    }
}