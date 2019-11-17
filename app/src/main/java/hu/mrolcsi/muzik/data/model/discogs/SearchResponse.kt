package hu.mrolcsi.muzik.data.model.discogs

import com.google.gson.annotations.SerializedName

data class SearchResponse(
  @SerializedName("pagination") val pagination: Pagination,
  @SerializedName("results") val results: List<SearchResult>
)