package hu.mrolcsi.muzik.data.model.discogs

import com.google.gson.annotations.SerializedName

data class Pagination(
  @SerializedName("per_page") val perPage: Int,
  @SerializedName("items") val items: Int,
  @SerializedName("pages") val pages: Int,
  @SerializedName("urls") val urls: Urls
) {

  data class Urls(
    @SerializedName("first") val first: String,
    @SerializedName("next") val next: String,
    @SerializedName("prev") val prev: String,
    @SerializedName("last") val last: String
  )
}