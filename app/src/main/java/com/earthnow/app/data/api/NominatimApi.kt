package com.earthnow.app.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * OpenStreetMap Nominatim reverse geocoding (free, no key). Usage policy
 * requires a descriptive User-Agent and low request rates; results are
 * cached for 24 h and requests are only made on explicit user taps.
 */
interface NominatimApi {
    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "jsonv2",
        @Query("zoom") zoom: Int = 10,
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("accept-language") acceptLanguage: String,
        @Header("User-Agent") userAgent: String = "EarthNow/1.0 (Android; personal project)"
    ): NominatimReverseDto
}

@JsonClass(generateAdapter = true)
data class NominatimReverseDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "address") val address: NominatimAddressDto? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class NominatimAddressDto(
    @Json(name = "city") val city: String? = null,
    @Json(name = "town") val town: String? = null,
    @Json(name = "village") val village: String? = null,
    @Json(name = "municipality") val municipality: String? = null,
    @Json(name = "county") val county: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null
)