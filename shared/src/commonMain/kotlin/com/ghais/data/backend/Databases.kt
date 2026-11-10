package com.ghais.data.backend

import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.json.*

/** The document wire format is retained while the Appwrite SDK is removed. */
data class Document<T>(val id: String, val data: T, val createdAt: String = "")
data class DocumentList(val documents: List<Document<Map<String, Any>>>)

object ID { fun unique(): String = "unique()" }
object Query {
    private fun make(method: String, attribute: String? = null, values: List<JsonElement>) = buildJsonObject {
        put("method", method)
        if (attribute != null) put("attribute", attribute)
        put("values", JsonArray(values))
    }.toString()
    fun equal(attribute: String, value: String) = make("equal", attribute, listOf(JsonPrimitive(value)))
    fun equal(attribute: String, value: Boolean) = make("equal", attribute, listOf(JsonPrimitive(value)))
    fun limit(value: Int) = make("limit", values = listOf(JsonPrimitive(value)))
    fun offset(value: Int) = make("offset", values = listOf(JsonPrimitive(value)))
    fun orderDesc(attribute: String) = make("orderDesc", attribute, emptyList())
    fun orderAsc(attribute: String) = make("orderAsc", attribute, emptyList())
    fun select(fields: List<String>) = make("select", values = fields.map { JsonPrimitive(it) })
}

class Databases {
    private fun path(databaseId: String, collectionId: String, documentId: String? = null): String =
        "/databases/${databaseId.encodeURLPathPart()}/collections/${collectionId.encodeURLPathPart()}/documents" +
            (documentId?.let { "/${it.encodeURLPathPart()}" } ?: "")
    private fun document(text: String): Document<Map<String, Any>> = document(Json.parseToJsonElement(text).jsonObject)
    private fun document(obj: JsonObject) = Document(obj.getValue("\$id").jsonPrimitive.content,
        obj.filterKeys { !it.startsWith('$') }.mapNotNull { (key, value) -> decode(value)?.let { key to it } }.toMap(), obj["\$createdAt"]?.jsonPrimitive?.content.orEmpty())
    suspend fun listDocuments(databaseId: String, collectionId: String, queries: List<String> = emptyList()): DocumentList {
        val obj = Json.parseToJsonElement(BackendHttp.request(HttpMethod.Get, path(databaseId, collectionId), queries = queries)).jsonObject
        return DocumentList(obj.getValue("documents").jsonArray.map { document(it.jsonObject) })
    }
    suspend fun getDocument(databaseId: String, collectionId: String, documentId: String, expectedUserId: String? = null) =
        document(BackendHttp.request(HttpMethod.Get, path(databaseId, collectionId, documentId), expectedUserId = expectedUserId))
    suspend fun createDocument(databaseId: String, collectionId: String, documentId: String, data: Map<String, Any?>, expectedUserId: String? = null) =
        document(BackendHttp.request(HttpMethod.Post, path(databaseId, collectionId), buildJsonObject {
            put("documentId", documentId); put("data", encode(data))
        }, expectedUserId = expectedUserId))
    suspend fun updateDocument(databaseId: String, collectionId: String, documentId: String, data: Map<String, Any?>) =
        document(BackendHttp.request(HttpMethod.Patch, path(databaseId, collectionId, documentId), buildJsonObject { put("data", encode(data)) }))
    suspend fun deleteDocument(databaseId: String, collectionId: String, documentId: String) {
        BackendHttp.request(HttpMethod.Delete, path(databaseId, collectionId, documentId))
    }
}

internal fun encode(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is Map<*, *> -> JsonObject(value.entries.associate { it.key.toString() to encode(it.value) })
    is Iterable<*> -> JsonArray(value.map(::encode))
    else -> error("Unsupported document value: ${value::class.simpleName}")
}
private fun decode(value: JsonElement): Any? = when (value) {
    JsonNull -> null
    is JsonObject -> value.mapValues { decode(it.value) }
    is JsonArray -> value.map(::decode)
    is JsonPrimitive -> if (value.isString) value.content else value.booleanOrNull ?: value.longOrNull ?: value.doubleOrNull ?: value.content
}
