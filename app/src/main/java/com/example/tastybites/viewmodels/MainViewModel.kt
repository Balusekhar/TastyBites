package com.example.tastybites.viewmodels

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
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
        if(hasInternetConnection()){
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
                Toast.makeText(getApplication(), "timeout", Toast.LENGTH_SHORT).show()
                return NetworkResult.Error(message = "TimeOut")
            }
            response.code() == 402 -> {
                Toast.makeText(getApplication(), "API Key limited", Toast.LENGTH_SHORT).show()
                return NetworkResult.Error(message = "API Key Limited")
            }
            response.code() == 401 -> {
                Toast.makeText(getApplication(), "You are not authorized. Check your API key.", Toast.LENGTH_SHORT).show()
                return NetworkResult.Error("You are not authorized. Check your API key.")
            }
            response.body()?.results.isNullOrEmpty() -> {
                Toast.makeText(getApplication(), "Recipes Not Found", Toast.LENGTH_SHORT).show()
                return NetworkResult.Error(message = "Recipes Not Found")
            }
            response.isSuccessful -> {
                val foodRecipe = response.body()
                Toast.makeText(getApplication(), "Recipes Found", Toast.LENGTH_SHORT).show()
                return NetworkResult.Success(foodRecipe!!)
            }
            else ->{
                Toast.makeText(getApplication(), "MY ERROR", Toast.LENGTH_SHORT).show()
                return NetworkResult.Error(response.message())
            }
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}