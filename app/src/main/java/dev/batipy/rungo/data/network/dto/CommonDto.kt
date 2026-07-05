package dev.batipy.rungo.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Retrofit sends a truly empty request for @POST calls with no @Body, and DRF's
 * default JSON parser errors on an empty payload ("Expecting value: line 1
 * column 1"). Passing this serializes to `{}` so those endpoints get valid JSON.
 */
@Serializable
class EmptyRequestBody
