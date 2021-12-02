## Testing

1. With and without internet

## UI Checklist

1. Minimum space between content and bottom of the window
1. List position is preserved on back


## Code Templates

### Interaction between ViewModel and Service

```kotlin

class ViewModel(private val service: Service) {

    init {
        service.itemsStateObservable.subscribeIO {
            ...
        }
        service.start()
    }

    override fun onCleared() {
        service.stop()
    }
}

class Service {
    private val itemsStateSubject = BehaviorSubject.create<DataState<Any>>()
    val itemsStateObservable: Observable<DataState<Any>> = itemsStateSubject

    private disposables = CompositeDisposables()

    fun start() {
        ...
    }

    fun stop() {
        disposables.clear()
    }
}
```
