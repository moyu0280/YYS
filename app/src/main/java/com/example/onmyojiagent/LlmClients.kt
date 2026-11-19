package com.example.onmyojiagent

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 本地 Ollama 客户端（离线模式，隐私优先） */
class OllamaClient(
    private val baseUrl: String = "http://127.0.0.1:11434",
    private val model: String = "qwen2.5:7b"
) : LlmClient {
    override fun complete(prompt: String): String {
        val body = JSONObject().put("model", model).put("prompt", prompt).put("stream", false)
        return post("$baseUrl/api/generate", body.toString())
            ?.let { JSONObject(it).optString("response") } ?: ""
    }
}

/** OpenAI 兼容云端客户端（GPT / Qwen / DeepSeek 等） */
class OpenAiCompatClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : LlmClient {
    override fun complete(prompt: String): String {
        val messages = JSONArray().put(JSONObject().put("role", "user").put("content", prompt))
        val body = JSONObject().put("model", model).put("messages", messages).put("temperature", 0.0)
        return post("$baseUrl/chat/completions", body.toString(), apiKey)
            ?.let {
                JSONObject(it).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").optString("content")
            } ?: ""
    }
}

/** 注意：必须在子线程调用（Android 主线程禁网络） */
private fun post(url: String, body: String, apiKey: String? = null): String? {
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        apiKey?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
        conn.outputStream.use { it.write(body.toByteArray()) }
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        null
    }
}
