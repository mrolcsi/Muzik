package hu.mrolcsi.android.lyricsplayer.extensions

import android.os.AsyncTask
import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations

fun <T, R> LiveData<T>.map(func: (T) -> LiveData<R>) = Transformations.map(this, func)

/**
 * Returns a `LiveData` mapped from the input `source` `LiveData` by applying
 * `mapFunction` to each value set on `source`.
 *
 *
 * This method is analogous to [io.reactivex.Observable.map].
 *
 * `transform` will be executed on a worker thread.
 *
 * Here is an example mapping a simple `User` struct in a `LiveData` to a
 * `LiveData` containing their full name as a `String`.
 *
 * <pre>
 * LiveData<User> userLiveData = ...;
 * LiveData<String> userFullNameLiveData =
 * Transformations.map(
 * userLiveData,
 * user -> user.firstName + user.lastName);
 * });
 * </pre>
 *
 * @param mapFunction a function to apply to each value set on `source` in order to set
 * it
 * on the output `LiveData`
 * @param <X>         the generic type parameter of `source`
 * @param <Y>         the generic type parameter of the returned `LiveData`
 * @return a LiveData mapped from `source` to type `<Y>` by applying
 * `mapFunction` to each value set.
 **/
fun <T, R> LiveData<T>.mapAsync(mapFunction: Function<T, R>) = _mapAsync(this, mapFunction)

fun <T, R> LiveData<T>.switchMap(func: (T) -> LiveData<R>) = Transformations.switchMap(this, func)

@MainThread
private fun <X, Y> _mapAsync(source: LiveData<X>, mapFunction: Function<X, Y>): LiveData<Y> {
  val result = MediatorLiveData<Y>()
  result.addSource(source) { x ->
    AsyncTask.execute {
      result.postValue(mapFunction.apply(x))
    }
  }
  return result
}