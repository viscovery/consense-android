package com.viscovery.ad.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface MockService {
    @GET("")
    Call<VmapResponse> getVmap(@Url String url);
}
