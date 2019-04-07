# Introduction

Simple image search app using the Flickr Image Search API. 

**Features**:
 - Endless scrolling of search results
 - 3 column grid view of images
 - Auto search as you type

## Demo
![enter image description here](https://github.com/arunkumar9t2/flickr-sample/raw/master/art/FlickrAppDemo.gif)

# Overview


 - **RxJava** - Used RxJava for easier concurrency and thread management. 
	Example:
	 - Searching: Ability to search as you type by debouncing keystrokes to avoid unnecessary requests
	 - Cancel existing network requests (both query and paging) when search query changes using `switchMap` operator.
	 - Manage a bound thread pool of size 6 to fetch and decode images concurrently.
 - **OkHttp** - Used for easier networking to fetch `json` and `input stream` for `Bitmap`s.

## Architecture
MVP with Repository to avoid logical code in `Activity` and separate data related code. Loose coupling by making code dependent on interfaces rather than implementations eg: `ImageCache`, `ImagesDataSource`, `ImageLoader`. Explicit dependency on Flickr is avoided and is accessed through contracts.

 - `data` - `ImagesDataSource` is a contract for a source that provides paged searching. `FlickrImagesDataSource` is an implementation backed by `Flickr API`. Also includes data classes for parsing `json` response.
 - `DepedencyInjector` - Utility class to manage dependencies throughout the app. Responsible for managing and providing dependencies when requested. Eg: `DependencyInjector.provideHomePresenter`.
 - `Image Loading` - `ImageLoader` and `ImageCache` are contracts for a system that loads images efficiently by reducing sample size, perform concurrent requests and caching.
 
		 - `DefaultImageLoader` - Loader backed by Rx to handle multiple requests and decode images.
		 - `MemoryImageCache` - Lru based memory cache to hold few `Bitmap`s in memory.
 - `SchedulerProvider` - Contract to provide various type of `Schedulers` for concurrency. Can be replaced during testing via DI.
 - `Home` - Home feature package that contains, presenter for communicating with data layer, adapter for rendering grid and `HomeActivity`
 - `Test` - Presenter test for search term achieved by injecting `MockImagesDataSource` and testing paging and searches.

# Possible improvements

- `ImageLoading`
		- While current implementation automatically uses `ImageViews` dimensions to downsize source image, there are other cases that could handled for better performance. Currently the load request is not cancelled when the `ImageView` is detached from window and relies on `RecyclerView.Adapter.onViewRecycled` to cancel the request, this could be changed to use `View.onAttachStateListener` to free one of the threads thereby improving load performance during scrolling.
		- Could use battle tested solutions like `Glide` or `Picasso`.
- `Paging`
		- By using [PagedList](https://developer.android.com/reference/android/arch/paging/PagedList) and [PageKeyedDataSource](https://developer.android.com/reference/android/arch/paging/PageKeyedDataSource) to simplify scroll listening, load more and concentrate on API implementations
- `Dagger 2` 
		- Use Dagger 2 instead of custom `DependencyInjector` to achieve compilation safety and scaling.
- `State Restoration`
		- Currently only the search term is restored upon a config change, this could be changed to use `ViewModel` pattern to cache the image list as well to avoid additional network calls.
- `Better Error handling` 
		- When a paged list fails to load, possibly a retry button could be used to restart failed requests.
- `Test` - Additional integration tests to avoid breakage due to 3rd parties.
- `LocalDataSource` - Implement a local data source for provide search results stored on disk.
