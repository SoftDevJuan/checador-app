package com.example.checadorccl.api.models

import com.google.gson.annotations.SerializedName

data class GoogleLoginRequest(
    @SerializedName("token") val token: String
)