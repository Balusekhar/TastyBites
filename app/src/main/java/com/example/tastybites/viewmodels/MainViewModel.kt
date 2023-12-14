package com.example.tastybites

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tastybites.data.Repository
import com.example.tastybites.models.FoodRecipe
import com.example.tastybites.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository,
    application: Application
) : AndroidViewModel(application) {

    var recipesResponse : MutableLiveData<NetworkResult<FoodRecipe>> = MutableLiveData()

    fun getRecipes(queries : Map<String,String>) = viewModelScope.launch {
        getRecipeSafeCall(queries)
    }

    private suspend fun getRecipeSafeCall(queries: Map<String, String>) {
        recipesResponse.value = NetworkResult.Loading()
        if(isInternetConnected()){
            try {
                val response = repository.remote.getRecipes(queries)
                recipesResponse.value = handleFoodRecipesResponse(response)
            }catch (e:Exception){
                recipesResponse.value = NetworkResult.Error("No Recipes Found")
            }
        }else{
            recipesResponse.value = NetworkResult.Error("No Internet")
        }
    }

    private fun handleFoodRecipesResponse(response: Response<FoodRecipe>): NetworkResult<FoodRecipe>? {
        when{
            response.message().toString().contains("timeout") -> {
                return NetworkResult.Error(message = "TimeOut")
            }
            response.code() == 402 -> {
                return NetworkResult.Error(message = "API Key Limited")
            }
            response.body()?.results.isNullOrEmpty() -> {
                return NetworkResult.Error(message = "Recipes Not Found")
            }
            response.isSuccessful -> {
                val foodRecipe = response.body()
                return NetworkResult.Success(foodRecipe!!)
            }
            else ->{
                return NetworkResult.Error(response.message())
            }
        }
    }

    fun isInternetConnected(): Boolean {
        val connectivityManager =
            getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Check for network capabilities for the active network
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        // Check if the network has internet connectivity
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}