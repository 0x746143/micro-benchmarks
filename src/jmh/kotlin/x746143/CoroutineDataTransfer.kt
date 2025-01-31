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
package x746143

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("FunctionName", "unused")
@Fork(3)
@Threads(1)
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 4, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 2, timeUnit = TimeUnit.SECONDS)
open class CoroutineDataTransfer {

    // These benchmarks use coroutines in a single-threaded mode,
    // similar to running them in a Netty event loop

    companion object {
        // If the `times` value is less than 10000
        // then the `runBlocking` calls affect test results
        private const val TIMES = 10000
    }

    var times = TIMES
    private var firstLetter = 'a'
    private var numberOfLetters = 26

    private var reusableBuffer: ByteBuffer = ByteBuffer.allocate(TIMES)
        get() {
            field.clear()
            return field
        }

    private val channel = Channel<Int>()

    private val emptyContinuation = object : Continuation<Unit> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {}
    }

    private fun startUninterceptedCoroutine(block: suspend () -> Unit) {
        block.createCoroutineUnintercepted(emptyContinuation).resume(Unit)
    }

    // ======== [ Evaluating the performance of coroutine creation and data transfer ] ========

    // Channel

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_a_Channel_a_DefaultLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            launch {
                val charCode = channel.receive()
                result.put((firstLetter + charCode).code.toByte())
            }
            channel.send(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_a_Channel_b_UndispLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                val charCode = channel.receive()
                result.put((firstLetter + charCode).code.toByte())
            }
            channel.send(result.position() % numberOfLetters)
            yield()
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_a_Channel_c_UnconfLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            launch(Dispatchers.Unconfined) {
                val charCode = channel.receive()
                result.put((firstLetter + charCode).code.toByte())
            }
            channel.send(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_a_Channel_d_UnconfUndispLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            launch(Dispatchers.Unconfined, CoroutineStart.UNDISPATCHED) {
                val charCode = channel.receive()
                result.put((firstLetter + charCode).code.toByte())
            }
            channel.send(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_a_Channel_e_UnintCoroutine() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            startUninterceptedCoroutine {
                val charCode = channel.receive()
                result.put((firstLetter + charCode).code.toByte())
            }
            channel.send(result.position() % numberOfLetters)
        }
        result
    }

    // Intercepted Continuation

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_b_IntCont_a_DefaultLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            lateinit var cont: Continuation<Int>
            launch {
                val charCode = suspendCoroutine { cont = it }
                result.put((firstLetter + charCode).code.toByte())
            }
            yield()
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_b_IntCont_b_UndispLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            lateinit var cont: Continuation<Int>
            launch(start = CoroutineStart.UNDISPATCHED) {
                val charCode = suspendCoroutine { cont = it }
                result.put((firstLetter + charCode).code.toByte())
            }
            cont.resume(result.position() % numberOfLetters)
            yield()
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_b_IntCont_c_UnconfLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            lateinit var cont: Continuation<Int>
            launch(Dispatchers.Unconfined) {
                val charCode = suspendCoroutine { cont = it }
                result.put((firstLetter + charCode).code.toByte())
            }
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_b_IntCont_d_UnconfUndispLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            lateinit var cont: Continuation<Int>
            launch(Dispatchers.Unconfined, CoroutineStart.UNDISPATCHED) {
                val charCode = suspendCoroutine { cont = it }
                result.put((firstLetter + charCode).code.toByte())
            }
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_b_IntCont_e_UnintCoroutine() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            lateinit var cont: Continuation<Int>
            startUninterceptedCoroutine {
                val charCode = suspendCoroutine { cont = it }
                result.put((firstLetter + charCode).code.toByte())
            }
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    // Unintercepted Continuation

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_c_UnintCont_a_DefaultLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            lateinit var cont: Continuation<Int>
            launch {
                val charCode = suspendCoroutineUninterceptedOrReturn {
                    cont = it
                    COROUTINE_SUSPENDED
                }
                result.put((firstLetter + charCode).code.toByte())
            }
            yield()
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_c_UnintCont_b_UndispLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            lateinit var cont: Continuation<Int>
            launch(start = CoroutineStart.UNDISPATCHED) {
                val charCode = suspendCoroutineUninterceptedOrReturn {
                    cont = it
                    COROUTINE_SUSPENDED
                }
                result.put((firstLetter + charCode).code.toByte())
            }
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_c_UnintCont_c_UnconfLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            lateinit var cont: Continuation<Int>
            launch(Dispatchers.Unconfined) {
                val charCode = suspendCoroutineUninterceptedOrReturn {
                    cont = it
                    COROUTINE_SUSPENDED
                }
                result.put((firstLetter + charCode).code.toByte())
            }
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_c_UnintCont_d_UnconfUndispLaunch() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            lateinit var cont: Continuation<Int>
            launch(Dispatchers.Unconfined, CoroutineStart.UNDISPATCHED) {
                val charCode = suspendCoroutineUninterceptedOrReturn {
                    cont = it
                    COROUTINE_SUSPENDED
                }
                result.put((firstLetter + charCode).code.toByte())
            }
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun creation_and_Transfer_using_c_UnintCont_e_UnintCoroutine() = runBlocking {
        val result = reusableBuffer
        repeat(times) {
            lateinit var cont: Continuation<Int>
            startUninterceptedCoroutine {
                val charCode = suspendCoroutineUninterceptedOrReturn {
                    cont = it
                    COROUTINE_SUSPENDED
                }
                result.put((firstLetter + charCode).code.toByte())
            }
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    // ======== [ Evaluating the performance of data transfer only ] ========

    // Channel

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_a_Channel_a_DefaultLaunch() = runBlocking {
        val result = reusableBuffer
        launch {
            repeat(times) {
                val charCode = channel.receive()
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            channel.send(result.position() % numberOfLetters)
            yield()
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_a_Channel_b_UndispLaunch() = runBlocking {
        val result = reusableBuffer
        launch(start = CoroutineStart.UNDISPATCHED) {
            repeat(times) {
                val charCode = channel.receive()
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            channel.send(result.position() % numberOfLetters)
            yield()
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_a_Channel_c_UnconfLaunch() = runBlocking {
        val result = reusableBuffer
        launch(Dispatchers.Unconfined) {
            repeat(times) {
                val charCode = channel.receive()
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            channel.send(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_a_Channel_d_UnconfUndispLaunch() = runBlocking {
        val result = reusableBuffer
        launch(Dispatchers.Unconfined, CoroutineStart.UNDISPATCHED) {
            repeat(times) {
                val charCode = channel.receive()
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            channel.send(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_a_Channel_e_UnintCoroutine() = runBlocking {
        val result = reusableBuffer
        startUninterceptedCoroutine {
            repeat(times) {
                val charCode = channel.receive()
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            channel.send(result.position() % numberOfLetters)
        }
        result
    }

    // Intercepted Continuation

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_b_IntCont_a_DefaultLaunch() = runBlocking {
        val result = reusableBuffer
        lateinit var cont: Continuation<Int>
        launch {
            repeat(times) {
                val charCode = suspendCoroutine { cont = it }
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            yield()
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_b_IntCont_b_UndispLaunch() = runBlocking {
        val result = reusableBuffer
        lateinit var cont: Continuation<Int>
        launch(start = CoroutineStart.UNDISPATCHED) {
            repeat(times) {
                val charCode = suspendCoroutine { cont = it }
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            cont.resume(result.position() % numberOfLetters)
            yield()
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_b_IntCont_c_UnconfLaunch() = runBlocking {
        val result = reusableBuffer
        lateinit var cont: Continuation<Int>
        launch(Dispatchers.Unconfined) {
            repeat(times) {
                val charCode = suspendCoroutine { cont = it }
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_b_IntCont_d_UnconfUndispLaunch() = runBlocking {
        val result = reusableBuffer
        lateinit var cont: Continuation<Int>
        launch(Dispatchers.Unconfined, CoroutineStart.UNDISPATCHED) {
            repeat(times) {
                val charCode = suspendCoroutine { cont = it }
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_b_IntCont_e_UnintCoroutine() = runBlocking {
        val result = reusableBuffer
        lateinit var cont: Continuation<Int>
        startUninterceptedCoroutine {
            repeat(times) {
                val charCode = suspendCoroutine { cont = it }
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    // Unintercepted Continuation

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_c_UnintCont_a_DefaultLaunch() = runBlocking {
        val result = reusableBuffer
        lateinit var cont: Continuation<Int>
        launch {
            repeat(times) {
                val charCode = suspendCoroutineUninterceptedOrReturn {
                    cont = it
                    COROUTINE_SUSPENDED
                }
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        yield()
        repeat(times) {
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_c_UnintCont_b_UndispLaunch() = runBlocking {
        val result = reusableBuffer
        lateinit var cont: Continuation<Int>
        launch(start = CoroutineStart.UNDISPATCHED) {
            repeat(times) {
                val charCode = suspendCoroutineUninterceptedOrReturn {
                    cont = it
                    COROUTINE_SUSPENDED
                }
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_c_UnintCont_c_UnconfLaunch() = runBlocking {
        val result = reusableBuffer
        lateinit var cont: Continuation<Int>
        launch(Dispatchers.Unconfined) {
            repeat(times) {
                val charCode = suspendCoroutineUninterceptedOrReturn {
                    cont = it
                    COROUTINE_SUSPENDED
                }
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_c_UnintCont_d_UnconfUndispLaunch() = runBlocking {
        val result = reusableBuffer
        lateinit var cont: Continuation<Int>
        launch(Dispatchers.Unconfined, CoroutineStart.UNDISPATCHED) {
            repeat(times) {
                val charCode = suspendCoroutineUninterceptedOrReturn {
                    cont = it
                    COROUTINE_SUSPENDED
                }
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_c_UnintCont_e_UnintCoroutine() = runBlocking {
        val result = reusableBuffer
        lateinit var cont: Continuation<Int>
        startUninterceptedCoroutine {
            repeat(times) {
                val charCode = suspendCoroutineUninterceptedOrReturn {
                    cont = it
                    COROUTINE_SUSPENDED
                }
                result.put((firstLetter + charCode).code.toByte())
            }
        }
        repeat(times) {
            cont.resume(result.position() % numberOfLetters)
        }
        result
    }

    private var callbackQueue = ArrayDeque<(Int) -> Unit>()
    @OperationsPerInvocation(TIMES)
    @Benchmark
    fun transfer_using_d_callbackQueue(): ByteBuffer {
        val result = reusableBuffer
        repeat(times) {
            callbackQueue.addLast { charCode -> result.put((firstLetter + charCode).code.toByte()) }
            callbackQueue.removeFirst()(result.position() % numberOfLetters)
        }
        return result
    }

    // This test is used to make sure that JVM doesn't apply any undesirable optimizations
    // related to the loop `repeat(times)` in `transfer_using_c_UnintCont_e_UnintCoroutine`
    // test and other Unintercepted Continuation tests. The results of the current test
    // should be lower than the results of `transfer_using_c_UnintCont_e_UnintCoroutine`.
    @Benchmark
    fun jvmOptimizationVerifier(bl: Blackhole) {
        lateinit var cont: Continuation<Int>
        startUninterceptedCoroutine {
            val charCode = suspendCoroutineUninterceptedOrReturn {
                cont = it
                COROUTINE_SUSPENDED
            }
            bl.consume((firstLetter + charCode).code.toByte())
        }
        cont.resume(times % numberOfLetters)
    }

    @Test
    fun testCoroutineDataTransfer() {
        val expected = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
        times = expected.length
        this::class.functions
            .filter { it.hasAnnotation<Benchmark>() }
            .filter { it.name != "jvmOptimizationVerifier" }
            .forEach {
                val actual = with(it.call(this) as ByteBuffer) { String(array(), 0, position()) }
                assertEquals(expected, actual, it.name)
            }
    }
}

/*
Benchmark                                                                            Mode  Cnt    Score   Error  Units
CoroutineDataTransfer.creation_and_Transfer_using_a_Channel_a_DefaultLaunch          avgt   16  378.519 ± 1.763  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_a_Channel_b_UndispLaunch           avgt   16  421.279 ± 1.416  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_a_Channel_c_UnconfLaunch           avgt   16  371.008 ± 4.203  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_a_Channel_d_UnconfUndispLaunch     avgt   16  376.538 ± 1.066  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_a_Channel_e_UnintCoroutine         avgt   16  125.002 ± 1.061  ns/op

CoroutineDataTransfer.creation_and_Transfer_using_b_IntCont_a_DefaultLaunch          avgt   16  349.024 ± 3.800  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_b_IntCont_b_UndispLaunch           avgt   16  333.337 ± 5.883  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_b_IntCont_c_UnconfLaunch           avgt   16  225.347 ± 1.917  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_b_IntCont_d_UnconfUndispLaunch     avgt   16  254.244 ± 9.685  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_b_IntCont_e_UnintCoroutine         avgt   16   31.648 ± 1.019  ns/op

CoroutineDataTransfer.creation_and_Transfer_using_c_UnintCont_a_DefaultLaunch        avgt   16  298.477 ± 1.724  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_c_UnintCont_b_UndispLaunch         avgt   16  130.717 ± 0.575  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_c_UnintCont_c_UnconfLaunch         avgt   16  206.223 ± 1.647  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_c_UnintCont_d_UnconfUndispLaunch   avgt   16  189.319 ± 1.390  ns/op
CoroutineDataTransfer.creation_and_Transfer_using_c_UnintCont_e_UnintCoroutine       avgt   16   11.016 ± 1.463  ns/op

CoroutineDataTransfer.transfer_using_a_Channel_a_DefaultLaunch                       avgt   16  177.241 ± 0.683  ns/op
CoroutineDataTransfer.transfer_using_a_Channel_b_UndispLaunch                        avgt   16  177.198 ± 0.921  ns/op
CoroutineDataTransfer.transfer_using_a_Channel_c_UnconfLaunch                        avgt   16  121.288 ± 2.868  ns/op
CoroutineDataTransfer.transfer_using_a_Channel_d_UnconfUndispLaunch                  avgt   16  116.066 ± 2.637  ns/op
CoroutineDataTransfer.transfer_using_a_Channel_e_UnintCoroutine                      avgt   16  106.929 ± 1.173  ns/op

CoroutineDataTransfer.transfer_using_b_IntCont_a_DefaultLaunch                       avgt   16   87.450 ± 1.548  ns/op
CoroutineDataTransfer.transfer_using_b_IntCont_b_UndispLaunch                        avgt   16   86.644 ± 0.498  ns/op
CoroutineDataTransfer.transfer_using_b_IntCont_c_UnconfLaunch                        avgt   16   28.244 ± 0.713  ns/op
CoroutineDataTransfer.transfer_using_b_IntCont_d_UnconfUndispLaunch                  avgt   16   28.810 ± 0.785  ns/op
CoroutineDataTransfer.transfer_using_b_IntCont_e_UnintCoroutine                      avgt   16   26.649 ± 0.137  ns/op

CoroutineDataTransfer.transfer_using_c_UnintCont_a_DefaultLaunch                     avgt   16    8.285 ± 0.334  ns/op
CoroutineDataTransfer.transfer_using_c_UnintCont_b_UndispLaunch                      avgt   16    8.376 ± 0.051  ns/op
CoroutineDataTransfer.transfer_using_c_UnintCont_c_UnconfLaunch                      avgt   16    8.449 ± 0.277  ns/op
CoroutineDataTransfer.transfer_using_c_UnintCont_d_UnconfUndispLaunch                avgt   16    8.412 ± 0.527  ns/op
CoroutineDataTransfer.transfer_using_c_UnintCont_e_UnintCoroutine                    avgt   16    8.465 ± 0.482  ns/op

CoroutineDataTransfer.transfer_using_d_callbackQueue                                 avgt   16   10.262 ± 0.646  ns/op
CoroutineDataTransfer.jvmOptimizationVerifier                                        avgt   16    7.097 ± 0.528  ns/op
*/