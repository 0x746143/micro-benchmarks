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

import sun.misc.Unsafe
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import java.lang.reflect.Field
import kotlin.reflect.KClass

fun getUnsafe(): Unsafe {
    return Unsafe::class.getField("theUnsafe").get(null) as Unsafe
}

@Suppress("DEPRECATION")
fun KClass<*>.getFieldOffset(name: String, unsafe: Unsafe): Long {
    return getField(name).let { unsafe.objectFieldOffset(it) }
}

fun KClass<*>.getField(name: String): Field {
    return java.getDeclaredField(name).apply { isAccessible = true }
}

fun KClass<*>.getVarHandle(name: String, type: Class<Long>?): VarHandle {
    return MethodHandles.privateLookupIn(java, MethodHandles.lookup()).findVarHandle(java, name, type)
}

fun getNativeFuncHandle(name: String, descriptor: FunctionDescriptor): MethodHandle {
    val linker = Linker.nativeLinker()
    return linker.defaultLookup().find(name)
        .map { linker.downcallHandle(it, descriptor) }
        .orElseThrow()
}