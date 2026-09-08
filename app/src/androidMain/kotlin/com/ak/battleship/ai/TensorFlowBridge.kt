package com.ak.battleship.ai

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

actual object TensorFlowBridge {
    
    private val cachedBuffers = mutableMapOf<String, MappedByteBuffer>()

    private fun getModelBuffer(context: Context, isOffense: Boolean, playerName: String): MappedByteBuffer {
        val fileName = if (isOffense) "moriarty_offense.tflite" else "moriarty_defense.tflite"
        return cachedBuffers.getOrPut(fileName) {
            val fd = context.assets.openFd(fileName)
            FileInputStream(fd.fileDescriptor).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
    }

    actual fun runMoriartyInference(
        context: Any?,
        isOffense: Boolean,
        playerName: String,
        flatData: FloatArray,
        steps: Int,
        numChannels: Int
    ): FloatArray? {
        val androidContext = context as? Context ?: return null
        
        val totalBytes = steps * numChannels * 10 * 10 * 4
        val inputBuffer = ByteBuffer.allocateDirect(totalBytes).apply {
            order(ByteOrder.nativeOrder())
        }
        flatData.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()

        val outputBuffer = Array(1) { FloatArray(100) }
        val interpreter = Interpreter(getModelBuffer(androidContext, isOffense, playerName))
        interpreter.run(inputBuffer, outputBuffer)
        interpreter.close()

        return outputBuffer[0]
    }

    actual fun isMoriartyAvailable(): Boolean = true
}
