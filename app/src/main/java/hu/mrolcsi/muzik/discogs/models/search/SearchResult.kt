package hu.mrolcsi.muzik.discogs.models.search

import com.google.gson.annotations.SerializedName

data class SearchResult(
  @SerializedName("id") val id: Long,
  @SerializedName("thumb") val thumb: String,
  @SerializedName("title") val title: String,
  @SerializedName("uri") val uri: String,
  @SerializedName("master_url") val masterUrl: String?,
  @SerializedName("cover_image") val coverImage: String,
  @SerializedName("resource_url") val resourceUrl: String,
  @SerializedName("master_id") val masterId: Long?,
  @SerializedName("type") val type: String
)