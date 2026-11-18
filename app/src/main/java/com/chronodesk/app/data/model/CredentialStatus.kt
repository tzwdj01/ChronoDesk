package com.chronodesk.app.data.model

import com.google.gson.annotations.SerializedName

enum class CredentialStatus {
    @SerializedName("valid")
    VALID,

    @SerializedName("expired")
    EXPIRED,

    @SerializedName("refresh_pending")
    REFRESH_PENDING,

    @SerializedName("not_logged_in")
    NOT_LOGGED_IN,

    @SerializedName("error")
    ERROR
}
