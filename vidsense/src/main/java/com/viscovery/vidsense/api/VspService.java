package com.viscovery.vidsense.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface VspService {
    @GET("api/vmap?platform=mobile&debug=0")
    Call<VmapResponse> getVmap(
            @Query(value = "api_key", encoded = true) String apiKey,
            @Query(value = "video_url", encoded = true) String videoUrl);
}
