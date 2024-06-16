package com.blum.bot

import io.ktor.client.request.*
import java.net.URL

fun io.ktor.http.HttpMessageBuilder.createHeaders(url: URL, content: String? = null) {
    headers {
        append("Accept", "application/json, text/plain, */*")
        append("Accept-Encoding", "gzip, deflate, br")
        append("Accept-Language", "en-GB,en;q=0.9")
        append("Connection", "keep-alive")
        if (content != null) {
            append("Content-Length", content.length.toString())
            append("Content-Type", "application/json")
        }
        append("Host", url.host)
        append("Origin", "https://telegram.blum.codes")
        append("Sec-Fetch-Dest", "empty")
        append("Sec-Fetch-Mode", "cors")
        append("Sec-Fetch-Site", "same-site")
        append("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148")
    }
}