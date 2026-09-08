package com.ak.battleship.ai

expect object TensorFlowBridge {
    fun runMoriartyInference(
        context: Any?, // Context on Android, null on Web
        isOffense: Boolean,
        playerName: String,
        flatData: FloatArray,
        steps: Int,
        numChannels: Int
    ): FloatArray?
    fun isMoriartyAvailable(): Boolean
}
