package com.ak.battleship.ai

actual object TensorFlowBridge {
    actual fun runMoriartyInference(
        context: Any?,
        isOffense: Boolean,
        playerName: String,
        flatData: FloatArray,
        steps: Int,
        numChannels: Int
    ): FloatArray? {
        // Fallback since TFLite does not exist on Wasm/Web
        return null
    }

    actual fun isMoriartyAvailable(): Boolean = false
}
