package hu.mrolcsi.muzik.discogs.models.search

import com.google.gson.annotations.SerializedName
import hu.mrolcsi.muzik.discogs.models.Pagination

data class SearchResponse(
  @SerializedName("pagination") val pagination: Pagination,
  @SerializedName("results") val results: Array<SearchResult>
)