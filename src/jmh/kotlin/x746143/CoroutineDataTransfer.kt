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
@Fork(4)
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
        private const val TIMES = 1000
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
    fun coroutine_and_Transfer_using_a_Channel_a_DefaultLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_a_Channel_b_UndispLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_a_Channel_c_UnconfLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_a_Channel_d_UnconfUndispLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_a_Channel_e_UnintCoroutine() = runBlocking {
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
    fun coroutine_and_Transfer_using_b_IntCont_a_DefaultLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_b_IntCont_b_UndispLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_b_IntCont_c_UnconfLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_b_IntCont_d_UnconfUndispLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_b_IntCont_e_UnintCoroutine() = runBlocking {
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
    fun coroutine_and_Transfer_using_c_UnintCont_a_DefaultLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_c_UnintCont_b_UndispLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_c_UnintCont_c_UnconfLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_c_UnintCont_d_UnconfUndispLaunch() = runBlocking {
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
    fun coroutine_and_Transfer_using_c_UnintCont_e_UnintCoroutine() = runBlocking {
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

    @Test
    fun testCoroutineDataTransfer() {
        val expected = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
        times = expected.length
        this::class.functions
            .filter { it.hasAnnotation<Benchmark>() }
            .forEach {
                val actual = with(it.call(this) as ByteBuffer) { String(array(), 0, position()) }
                assertEquals(expected, actual, it.name)
            }
    }
}
