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

import java.nio.ByteBuffer

@Suppress("unused")
class JniAdapter {
    init {
        System.loadLibrary("JniAdapter")
    }
    external fun getPid(): Int
    external fun memcpyByObject(dest: ByteBuffer, src: ByteBuffer, length: Int)
    external fun memcpyByAddress(destAddress: Long, srcAddress: Long, length: Int)
}
