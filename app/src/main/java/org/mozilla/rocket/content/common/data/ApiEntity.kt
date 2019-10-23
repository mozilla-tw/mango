package org.mozilla.rocket.content.common.data

import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.rocket.util.toJsonObject

data class ApiEntity(
    val version: Long,
    val subcategories: List<ApiCategory>
) {

    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(KEY_VERSION, version)
        val jsonArray = JSONArray()
        for (subcategory in subcategories) {
            jsonArray.put(subcategory.toJsonObject())
        }
        jsonObject.put(KEY_SUBCATEGORIES, jsonArray)
        return jsonObject
    }

    companion object {
        private const val KEY_VERSION = "version"
        private const val KEY_SUBCATEGORIES = "subcategories"

        fun fromJson(jsonString: String?): ApiEntity {
            return if (jsonString != null) {
                val jsonObject = jsonString.toJsonObject()
                val jsonArray = jsonObject.optJSONArray(KEY_SUBCATEGORIES)
                val subcategories =
                    (0 until jsonArray.length())
                        .map { index -> jsonArray.getJSONObject(index) }
                        .map { jObj -> ApiCategory.fromJson(jObj) }

                ApiEntity(
                    jsonObject.optLong(KEY_VERSION),
                    subcategories
                )
            } else {
                ApiEntity(1, emptyList())
            }
        }
    }
}

data class ApiCategory(
    val componentType: String,
    val subcategoryName: String,
    val subcategoryId: Int,
    val items: List<ApiItem>
) {

    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(KEY_COMPONENT_TYPE, componentType)
        jsonObject.put(KEY_SUBCATEGORY_NAME, subcategoryName)
        jsonObject.put(KEY_SUBCATEGORY_ID, subcategoryId)
        val jsonArray = JSONArray()
        for (apiItem in items) {
            jsonArray.put(apiItem.toJsonObject())
        }
        jsonObject.put(KEY_ITEMS, jsonArray)
        return jsonObject
    }

    companion object {
        private const val KEY_COMPONENT_TYPE = "componentType"
        private const val KEY_SUBCATEGORY_NAME = "subcategoryName"
        private const val KEY_SUBCATEGORY_ID = "subcategoryId"
        private const val KEY_ITEMS = "items"

        fun fromJson(jsonObject: JSONObject): ApiCategory {
            val jsonArray = jsonObject.optJSONArray(KEY_ITEMS)
            val items =
                (0 until jsonArray.length())
                    .map { index -> jsonArray.getJSONObject(index) }
                    .map { jObj -> ApiItem.fromJson(jObj) }

            return ApiCategory(
                jsonObject.optString(KEY_COMPONENT_TYPE),
                jsonObject.optString(KEY_SUBCATEGORY_NAME),
                jsonObject.optInt(KEY_SUBCATEGORY_ID),
                items
            )
        }
    }
}

data class ApiItem(
    val sourceName: String,
    val image: String,
    val destination: String,
    val title: String,
    val componentId: String,
    val price: String = "",
    var discount: String = "",
    var score: Float = 0F,
    var scoreReviews: String = "",
    var description: String = "",
    var created_at: Long? = 0L,
    var country: String? = "",
    var sourceType: String? = "",
    var partner: Boolean? = false,
    var verticalName: String? = "",
    var verticalId: Int? = 0,
    var categoryName: String? = "",
    var categoryId: Int? = 0,
    var subcategoryName: String? = "",
    var subcategoryId: Int? = 0,
    var componentTypeName: String? = "",
    var componentTypeId: Int? = 0,
    var imageType: String? = "",
    var fresh: Boolean? = false,
    var startDate: Long? = 0L,
    var endDate: Long? = 0L,
    var additional: String = ""
) {

    fun toJsonObject(): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put(KEY_SOURCE_NAME, sourceName)
        jsonObject.put(KEY_IMAGE, image)
        jsonObject.put(KEY_DESTINATION, destination)
        jsonObject.put(KEY_TITLE, title)
        jsonObject.put(KEY_COMPONENT_ID, componentId)
        jsonObject.put(KEY_PRICE, price)
        jsonObject.put(KEY_DISCOUNT, discount)
        jsonObject.put(KEY_SCORE, score)
        jsonObject.put(KEY_SCORE_REVIEWS, scoreReviews)
        jsonObject.put(KEY_DESCRIPTION, description)
        jsonObject.put(KEY_CREATED_AT, created_at)
        jsonObject.put(KEY_COUNTRY, country)
        jsonObject.put(KEY_SOURCE_TYPE, sourceType)
        jsonObject.put(KEY_PARTNER, partner)
        jsonObject.put(KEY_VERTICAL_NAME, verticalName)
        jsonObject.put(KEY_VERTICAL_ID, verticalId)
        jsonObject.put(KEY_CATEGORY_NAME, categoryName)
        jsonObject.put(KEY_CATEGORY_ID, categoryId)
        jsonObject.put(KEY_SUBCATEGORY_NAME, subcategoryName)
        jsonObject.put(KEY_SUBCATEGORY_ID, subcategoryId)
        jsonObject.put(KEY_COMPONENT_TYPE_NAME, componentTypeName)
        jsonObject.put(KEY_COMPONENT_TYPE_ID, componentTypeId)
        jsonObject.put(KEY_IMAGE_TYPE, imageType)
        jsonObject.put(KEY_FRESH, fresh)
        jsonObject.put(KEY_START_DATE, startDate)
        jsonObject.put(KEY_END_DATE, endDate)
        jsonObject.put(KEY_ADDITIONAL, additional)
        return jsonObject
    }

    companion object {
        private const val KEY_SOURCE_NAME = "source_name"
        private const val KEY_IMAGE = "image"
        private const val KEY_DESTINATION = "destination"
        private const val KEY_TITLE = "title"
        private const val KEY_COMPONENT_ID = "component_id"
        private const val KEY_PRICE = "price"
        private const val KEY_DISCOUNT = "discount"
        private const val KEY_SCORE = "score"
        private const val KEY_SCORE_REVIEWS = "score_reviews"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_COUNTRY = "country"
        private const val KEY_SOURCE_TYPE = "source_type"
        private const val KEY_PARTNER = "partner"
        private const val KEY_VERTICAL_NAME = "vertical_name"
        private const val KEY_VERTICAL_ID = "vertical_id"
        private const val KEY_CATEGORY_NAME = "category_name"
        private const val KEY_CATEGORY_ID = "category_id"
        private const val KEY_SUBCATEGORY_NAME = "subcategory_name"
        private const val KEY_SUBCATEGORY_ID = "subcategory_id"
        private const val KEY_COMPONENT_TYPE_NAME = "component_type_name"
        private const val KEY_COMPONENT_TYPE_ID = "component_type_id"
        private const val KEY_IMAGE_TYPE = "image_type"
        private const val KEY_FRESH = "fresh"
        private const val KEY_START_DATE = "start_date"
        private const val KEY_END_DATE = "end_date"
        private const val KEY_ADDITIONAL = "additional"

        fun fromJson(jsonObject: JSONObject): ApiItem =
            ApiItem(
                jsonObject.optString(KEY_SOURCE_NAME),
                jsonObject.optString(KEY_IMAGE),
                jsonObject.optString(KEY_DESTINATION),
                jsonObject.optString(KEY_TITLE),
                jsonObject.optString(KEY_COMPONENT_ID),
                jsonObject.optString(KEY_PRICE),
                jsonObject.optString(KEY_DISCOUNT),
                jsonObject.optDouble(KEY_SCORE, 0.toDouble()).toFloat(),
                jsonObject.optString(KEY_SCORE_REVIEWS),
                jsonObject.optString(KEY_DESCRIPTION),
                jsonObject.optLong(KEY_CREATED_AT),
                jsonObject.optString(KEY_COUNTRY),
                jsonObject.optString(KEY_SOURCE_TYPE),
                jsonObject.optBoolean(KEY_PARTNER),
                jsonObject.optString(KEY_VERTICAL_NAME),
                jsonObject.optInt(KEY_VERTICAL_ID),
                jsonObject.optString(KEY_CATEGORY_NAME),
                jsonObject.optInt(KEY_CATEGORY_ID),
                jsonObject.optString(KEY_SUBCATEGORY_NAME),
                jsonObject.optInt(KEY_SUBCATEGORY_ID),
                jsonObject.optString(KEY_COMPONENT_TYPE_NAME),
                jsonObject.optInt(KEY_COMPONENT_TYPE_ID),
                jsonObject.optString(KEY_IMAGE_TYPE),
                jsonObject.optBoolean(KEY_FRESH),
                jsonObject.optLong(KEY_START_DATE),
                jsonObject.optLong(KEY_END_DATE),
                jsonObject.optString(KEY_ADDITIONAL)
            )
    }
}
