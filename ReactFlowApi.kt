package com.reactflow.app

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PostItem(val id: String, val message: String, val permalink: String, val target: String, val reactionCount: String? = null)

class ReactFlowApi(private val baseUrl: String) {
    private val client = OkHttpClient()

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(baseUrl.trimEnd('/') + path).get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = try { JSONObject(body).optString("error") } catch (_: Exception) { body }
                error("API ${response.code}: $detail")
            }
            body
        }
    }

    suspend fun getPages(session: String): List<String> {
        val json = JSONObject(get("api/session/$session/pages"))
        val data = json.optJSONArray("data") ?: JSONArray()
        return (0 until data.length()).map { i ->
            val item = data.getJSONObject(i)
            "${item.optString("name")} (${item.optString("id")})"
        }
    }

    suspend fun getReactionCount(session: String, postId: String, target: String): String {
        val json = JSONObject(get("api/session/$session/reactions/$postId?target=$target"))
        return json.optJSONObject("summary")?.optString("total_count") ?: "Unavailable"
    }

    suspend fun getPosts(session: String, target: String): List<PostItem> {
        val json = JSONObject(get("api/session/$session/posts?target=$target"))
        val data = json.optJSONArray("data") ?: JSONArray()
        return (0 until data.length()).map { i ->
            val item = data.getJSONObject(i)
            PostItem(
                item.optString("id"),
                item.optString("message"),
                item.optString("permalink_url", "https://www.facebook.com/"),
                target
            )
        }
    }
}
