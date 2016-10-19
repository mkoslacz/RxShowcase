package com.mateuszkoslacz.rxshowcase;

import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmObject;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

public class RxJavaShowcasingTests {

    @Test
    public void emittingSingleItem() throws Exception {
        Observable.just("emit this string")
                .subscribe(System.out::println);
    }

    @Test
    public void emittingMultipleItems() throws Exception {
        List<String> stringList = Arrays.asList("first", "second", "third");

        Observable.from(stringList)
                .subscribe(System.out::println);

        // what will happen if we use Observable.just ?
    }

    @Test
    public void filteringItems() throws Exception {
        List<Integer> integerList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Observable.from(integerList)
                .filter(outputInt -> outputInt % 2 == 0)
                .subscribe(System.out::println);

        // what exactly does the filter function do to the stream elements?
        // what will happen if we use != 0 ?
    }

    @Test
    public void applyingFunctionToItems() throws Exception {
        List<Integer> integerList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Observable.from(integerList)
                .map(outputInt -> Math.pow(outputInt, 2))
                .subscribe(System.out::println);

        // what exactly does the map function do to the stream elements?
        // what will happen if we use even numbers filter that we used in the previous test in map?
    }

    @Test
    public void forkingStreams() throws Exception {
        List<Integer> firstIntegerList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> secondIntegerList = Arrays.asList(90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100);
        List<Integer> thirdIntegerList = Arrays.asList(990, 991, 992, 993, 994, 995, 996, 997, 998,
                999, 1000);
        List<List<Integer>> lists = Arrays.asList(firstIntegerList, secondIntegerList, thirdIntegerList);

        Observable.from(lists)
                .flatMap(Observable::from)
                .subscribe(System.out::println);

        // what will happen if we use the map method here?
        // what will be a "standard" implementation of that?
        // there are also concatMap and switchMap available
    }

    @Test
    public void collectingEmittedObjectsToList() throws Exception {
        List<Integer> firstIntegerList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> secondIntegerList = Arrays.asList(90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100);
        List<Integer> thirdIntegerList = Arrays.asList(990, 991, 992, 993, 994, 995, 996, 997, 998,
                999, 1000);
        List<List<Integer>> lists = Arrays.asList(firstIntegerList, secondIntegerList, thirdIntegerList);

        Observable.from(lists)
                .flatMap(Observable::from)
                .toList()
                .subscribe(System.out::println);
    }

    @Test
    public void customCollectingEmittedObjects() throws Exception {
        List<Integer> resultList = new ArrayList<>();

        List<Integer> firstIntegerList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> secondIntegerList = Arrays.asList(90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100);
        List<Integer> thirdIntegerList = Arrays.asList(990, 991, 992, 993, 994, 995, 996, 997, 998,
                999, 1000);
        List<List<Integer>> lists = Arrays.asList(firstIntegerList, secondIntegerList, thirdIntegerList);

        Observable.from(lists)
                .flatMap(Observable::from)
                .collect(() -> resultList, (integers, integer) -> integers.add(integer + 1))
                .subscribe(System.out::println);

        // what is the meaning of particular collect lambdas elements?
        // how can we make result list to be interlaced with zeros?
    }

    @Test
    public void easyThreading() throws Exception {
        List<String> stringList = Arrays.asList("first", "second", "third");
        long startTime = System.currentTimeMillis();

        Observable.from(stringList)
                .map(this::doHeavyWork)
                .subscribeOn(Schedulers.computation()) // just sent the work to nicely managed thread pool
                .observeOn(Schedulers.immediate()) // and published it to "ui" thread
                .subscribe(System.out::println,
                        Throwable::printStackTrace);

        System.out.println("Execution time: " + (System.currentTimeMillis() - startTime));

        // what will happen if we rem-out subscribeOn adn observeOn
        // why doesn't it print anything?
    }

    @Test
    public void convertingCallbacksToObservables() throws Exception {
        // won't work, just a concept presentation
        // [...]
        Realm realm = Realm.getDefaultInstance();
        RealmObject sampleRealmObject = (RealmObject) new Object(); // fake

        Observable.create(subscriber -> {
            realm.executeTransactionAsync(
                    realm1 -> { // transaction
                        realm1.copyToRealm(sampleRealmObject);
                    },
                    () -> { // onComplete
                        if (subscriberIsValid(subscriber)) {
                            // if we would like to create some objects
                            // async and then pass them to subscriber
//                            subscriber.onNext(someobject);
                            subscriber.onCompleted();
                        }
                    },
                    error -> { // onError
                        if (subscriberIsValid(subscriber)) {
                            subscriber.onError(error);
                        }
                    });
        });
    }

    @Test
    public void presentingAllKindsOfCallbacks() throws Exception {
        List<String> stringList = Arrays.asList("first", "second", "third");

        Observable.from(stringList)
                .doOnNext(string -> System.out.println("string length: " + string.length())) // here is doOnNext
                .subscribe(
                        System.out::println, // onNext
                        throwable -> System.out.println("error received! " +  // onError
                                throwable.getClass().getName()),
                        () -> System.out.println("Observable completed!")); // onComplete

        // what will happen if we change one of the strings to null?
    }

    @Test
    public void combiningResultsFromTwoAsyncSources() throws Exception {
        List<String> stringList = Arrays.asList("first", "second", "third");
        List<String> secondStringList = Arrays.asList("A", "B", "C");

        Observable<String> firstListObservable = Observable.from(stringList)
                .map(this::doHeavyWork); // this one emits items pretty slowly

        Observable<String> secondListObservable = Observable.from(secondStringList); // this one is quick

        Observable.zip(firstListObservable, secondListObservable,
                (stringFromFirstList, stringFromSecondList) -> // onNext
                        stringFromFirstList + " and zipped with " + stringFromSecondList)
                .subscribe(System.out::println); // synchronized!
    }

    @Test
    public void hotVsColdObservables() throws Exception {
        // hot ones (ui user clicks, database updates, etc.), doing stuff whenever they want
        Observable<Long> hotObservable = createHotLongObservable();
        // cold ones, (created from lists etc.), doing stuff after subscription
        Observable<Long> coldObservable = createColdLongObservable();

        Thread.sleep(1000);

        System.out.println("subscribing to hot observable...");
        hotObservable.subscribe(aLong -> System.out.println("received hot " + aLong));

        System.out.println("subscribing to cold observable...");
        coldObservable.subscribe(aLong -> System.out.println("received cold " + aLong));

        Thread.sleep(1000);
    }

    @Test
    public void waitingForInitializationOfComponent() throws Exception {
        // view init
        ReplaySubject<Long> magicHappensHere = ReplaySubject.create();
        Observable<Long> userButtonClicksFromUi = createHotLongObservable();
        userButtonClicksFromUi.subscribe(magicHappensHere);
        // end of view init (some actions happen from now)

        //presenter long init
        Subscriber<Long> presenterThatInitializesSlowly = new Subscriber<Long>() {
            @Override
            public void onCompleted() {
                //stub
            }

            @Override
            public void onError(Throwable e) {
                //stub
            }

            @Override
            public void onNext(Long aLong) {
                System.out.println("presenter received " + aLong);
            }
        };

        Thread.sleep(1000); // fake long initialization
        //end of presenter init

        System.out.println("subscribing to ReplaySubject...");
        magicHappensHere.subscribe(presenterThatInitializesSlowly); // all cached!

        Thread.sleep(1000);
    }

    private boolean subscriberIsValid(Subscriber<? super Object> subscriber) {
        return subscriber != null && !subscriber.isUnsubscribed();
    }

    private String doHeavyWork(String input) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return input + " was processed";
    }

    @NonNull
    private Observable<Long> createColdLongObservable() {
        return Observable.interval(200, TimeUnit.MILLISECONDS)
                .doOnNext(aLong -> System.out.println("a birth of cold " + aLong))
                .subscribeOn(Schedulers.io());
    }

    @NonNull
    private Observable<Long> createHotLongObservable() {
        Observable<Long> hotObservableParent =
                Observable.interval(200, TimeUnit.MILLISECONDS)
                        .doOnNext(aLong -> System.out.println("a birth of hot " + aLong))
                        .subscribeOn(Schedulers.io());
        PublishSubject<Long> hotObservable = PublishSubject.create();
        hotObservableParent.subscribe(hotObservable);
        return hotObservable;
    }
}