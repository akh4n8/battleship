package com.ak.battleship.network

actual class TelemetryClient actual constructor() {
    actual fun sendMatchData(url: String, anonKey: String, payload: String) {
        if (url.isBlank() || anonKey.isBlank()) return
        jsPost(url, anonKey, payload)
    }
}

private fun jsPost(url: String, key: String, bodyStr: String) {
    js("""
        fetch(url + '/rest/v1/game_telemetry', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'apikey': key,
                'Authorization': 'Bearer ' + key
            },
            body: bodyStr
        }).then(response => {
            if (!response.ok) {
                console.error("Telemetry error", response.status);
            }
        }).catch(e => console.error("Telemetry fetch error", e));
    """)
}
