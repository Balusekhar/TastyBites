package com.example.tastybites.data

import com.example.tastybites.data.network.FoodRecipesApi
import com.example.tastybites.models.FoodRecipe
import retrofit2.Response
import javax.inject.Inject

class RemoteDataSource @Inject constructor(
    private val foodRecipesApi: FoodRecipesApi
){

    suspend fun getRecipes(queries : Map<String,String>) : Response<FoodRecipe>{
        return foodRecipesApi.getRecipes(queries)
    }
}