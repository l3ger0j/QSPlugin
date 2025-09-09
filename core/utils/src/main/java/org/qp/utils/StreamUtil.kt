package org.qp.utils

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object StreamUtil {
    private const val BUFFER_SIZE = 8192

    /**
     * Copies data from the `from` stream to the `to` stream.
     */
    @Throws(IOException::class)
    fun copy(from: InputStream, to: OutputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while ((from.read(buffer).also { bytesRead = it }) > 0) {
            to.write(buffer, 0, bytesRead)
        }
    }
}
