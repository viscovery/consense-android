package com.viscovery.ad.vast;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface VastService {
    @GET("")
    Call<Vast> getDocument(@Url String url);
}
