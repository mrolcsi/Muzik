package hu.mrolcsi.android.lyricsplayer.extensions

import android.os.AsyncTask
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations

/**
 * Returns a `LiveData` mapped from the input `source` `LiveData` by applying
 * `mapFunction` to each value set on `source˙.
 *
 * This method is analogous to [io.reactivex.Observable#map].
 *
 * `transform` will be executed on a worker thread.
 *
 * Here is an example mapping a simple `User` struct in a `LiveData` to a
 * `LiveData` containing their full name as a `String`.
 *
 * <pre>
 * LiveData<User> userLiveData = ...;
 * LiveData<String> userFullNameLiveData =
 *     Transformations.map(
 *         userLiveData,
 *         user -> user.firstName + user.lastName);
 * });
 * </pre>
 *
 * @param mapFunction a function to apply to each value set on `source` in order to set
 *                    it
 *                    on the output `LiveData`
 * @param T           the generic type parameter of `source`
 * @param R           the generic type parameter of the returned `LiveData`
 * @return a LiveData mapped from `source` to type `<Y>` by applying
 * `mapFunction` to each value set.
 */
@MainThread
fun <T, R> LiveData<T>.map(mapFunction: (T) -> LiveData<R>) = Transformations.map(this, mapFunction)

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
 *                    it
 *                    on the output `LiveData`
 * @param T           the generic type parameter of `source`
 * @param R           the generic type parameter of the returned `LiveData`
 * @return a LiveData mapped from `source` to type `<Y>` by applying`mapFunction` to each value set.
 **/
@MainThread
fun <T, R> LiveData<T>.mapAsync(mapFunction: (T) -> R): LiveData<R> {
  val result = MediatorLiveData<R>()
  result.addSource(this) { x ->
    AsyncTask.execute {
      result.postValue(mapFunction.invoke(x))
    }
  }
  return result
}

/**
 * Returns a `LiveData` mapped from the input `source` `LiveData` by applying
 * `switchMapFunction` to each value set on `source`.
 *
 * The returned `LiveData` delegates to the most recent `LiveData` created by
 * calling `switchMapFunction` with the most recent value set to `source`, without
 * changing the reference. In this way, `switchMapFunction` can change the 'backing'
 * `LiveData` transparently to any observer registered to the `LiveData` returned
 * by `switchMap()`.
 *
 * Note that when the backing `LiveData` is switched, no further values from the older
 * `LiveData` will be set to the output `LiveData`. In this way, the method is
 * analogous to [io.reactivex.Observable#switchMap].
 *
 * `switchMapFunction` will be executed on the main thread.
 *
 * Here is an example class that holds a typed-in name of a user
 * `String` (such as from an `EditText`) in a {@link MutableLiveData` and
 * returns a `LiveData` containing a List of `User` objects for users that have
 * that name. It populates that `LiveData` by requerying a repository-pattern object
 * each time the typed name changes.
 *
 * This `ViewModel` would permit the observing UI to update "live" as the user ID text
 * changes.
 *
 * <pre>
 * class UserViewModel extends AndroidViewModel {
 *     MutableLiveData<String> nameQueryLiveData = ...
 *
 *     LiveData<List<String>> getUsersWithNameLiveData() {
 *         return Transformations.switchMap(
 *             nameQueryLiveData,
 *                 name -> myDataSource.getUsersWithNameLiveData(name));
 *     }
 *
 *     void setNameQuery(String name) {
 *         this.nameQueryLiveData.setValue(name);
 *     }
 * }
 * </pre>
 *
 * @param switchMapFunction a function to apply to each value set on `source` to create a
 *                          new delegate `LiveData` for the returned one
 * @param T                 the generic type parameter of `source`
 * @param R                 the generic type parameter of the returned `LiveData`
 * @return a LiveData mapped from `source` to type `<Y>` by delegating
 * to the LiveData returned by applying `switchMapFunction` to each
 * value set
 */
@MainThread
fun <T, R> LiveData<T>.switchMap(switchMapFunction: (T) -> LiveData<R>): LiveData<R> =
  Transformations.switchMap(this, switchMapFunction)