package com.chessanalyzer.data.api

import com.chessanalyzer.data.model.EngineResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

interface ChessEngineApi {
    
    @GET("analyze")
    suspend fun analyzeFEN(
        @Query("fen") fen: String,
        @Query("depth") depth: Int
    ): Response<EngineResponse>
    
    companion object {
        private const val BASE_URL = "https://chess-api.com/v1/"
        
        fun create(): ChessEngineApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
            
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(ChessEngineApi::class.java)
        }
    }
}
