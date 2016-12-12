package com.mateuszkoslacz.rxshowcase;

import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import io.realm.Realm;
import io.realm.RealmObject;


public class RxJavaShowcasingTests {

    @Test
    public void emittingSingleItem() throws Exception {
        Observable.just("emit this string")
                .subscribe(System.out::println);
    }

    @Test
    public void emittingMultipleItems() throws Exception {
        List<String> stringList = Arrays.asList("first", "second", "third");

        Observable.fromIterable(stringList)
                .subscribe(System.out::println);

        // what will happen if we use Observable.just ?
    }

    @Test
    public void filteringItems() throws Exception {
        List<Integer> integerList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Observable.fromIterable(integerList)
                .filter(outputInt -> outputInt % 2 == 0)
                .subscribe(System.out::println);

        // what exactly does the filter function do to the stream elements?
        // what will happen if we use != 0 ?
    }

    @Test
    public void applyingFunctionToItems() throws Exception {
        List<Integer> integerList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Observable.fromIterable(integerList)
                .map(outputInt -> Math.pow(outputInt, 2))
                .subscribe(
                        System.out::println,
                        Throwable::printStackTrace,
                        () -> System.out.println("completed"));

        // what exactly does the map function do to the stream elements?
        // what will happen if we use even numbers filter that we used in the previous test in map?
    }

    @Test
    public void mergingStreams() throws Exception {
        List<Integer> firstIntegerList = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> secondIntegerList = Arrays.asList(90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100);
        List<Integer> thirdIntegerList = Arrays.asList(990, 991, 992, 993, 994, 995, 996, 997, 998,
                999, 1000);
        List<List<Integer>> lists = Arrays.asList(firstIntegerList, secondIntegerList, thirdIntegerList);

        Observable.fromIterable(lists)
                .flatMap((list) -> Observable.fromIterable(list))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.trampoline())
                .subscribe(System.out::println);
        Thread.sleep(3000);

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
        Observable.fromIterable(lists)
                .flatMap(Observable::fromIterable)
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

        Observable.fromIterable(lists)
                .flatMap(Observable::fromIterable)
                .collect(() -> resultList, (resultList1, integer) -> resultList1.add(integer + 100))
                .subscribe(System.out::println);

        // what is the meaning of particular collect lambdas elements?
        // how can we make result list to be interlaced with zeros?
    }

    @Test
    public void easyThreading() throws Exception {
        List<String> stringList = Arrays.asList("first", "second", "third");
        long startTime = System.currentTimeMillis();

        Observable.fromIterable(stringList)
                .map(this::doHeavyWork)
                .subscribeOn(Schedulers.computation()) // just sent the work to nicely managed thread pool
                .observeOn(Schedulers.trampoline()) // and published it to "ui" thread
                .subscribe(System.out::println,
                        Throwable::printStackTrace);

        System.out.println("Execution time: " + (System.currentTimeMillis() - startTime));

        // what will happen if we rem-out subscribeOn adn observeOn
        // why doesn't it print anything? what to do to avoid it?
    }

    @Test
    public void convertingCallbacksToCompletables() throws Exception {
        // won't work, just a concept presentation
        // [...]
        Realm realm = Realm.getDefaultInstance();
        RealmObject sampleRealmObject = (RealmObject) new Object(); // fake

        Completable.create(emitter ->
                realm.executeTransactionAsync(
                        realm1 -> realm1.copyToRealm(sampleRealmObject),
                        emitter::onComplete,
                        emitter::onError));
    }

    @Test
    public void combiningResultsFromTwoAsyncSources() throws Exception {
        List<String> stringList = Arrays.asList("first", "second", "third");
        List<String> secondStringList = Arrays.asList("A", "B", "C");

        Observable<String> firstListObservable = Observable.fromIterable(stringList) // this one emits items pretty slowly
                .subscribeOn(Schedulers.io())
                .map(this::doHeavyWork);

        Observable<String> secondListObservable = Observable.fromIterable(secondStringList) // this one is quick
                .subscribeOn(Schedulers.io());

        Observable.zip(firstListObservable, secondListObservable,
                (stringFromFirstList, stringFromSecondList) -> // onNext
                        stringFromFirstList + " and zipped with " + stringFromSecondList)
                .subscribe(System.out::println); // synchronized!

        Thread.sleep(5000);

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

        //fake presenter
        Observer<Long> observer = new Observer<Long>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Long value) {
                System.out.println("presenter received " + value);

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };


        Thread.sleep(1000); // fake long initialization
        //end of presenter init

        System.out.println("subscribing to ReplaySubject...");
        magicHappensHere.subscribe(observer); // all cached!

        Thread.sleep(1000);
    }


    ///////////////////////////////////////////////////////////////////////////
    // THE END
    ///////////////////////////////////////////////////////////////////////////


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