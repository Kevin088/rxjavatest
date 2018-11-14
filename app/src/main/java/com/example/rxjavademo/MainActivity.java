package com.example.rxjavademo;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.annimon.stream.Stream;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {
    Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //创建被观察者对象
        /**
         * create
         */
        Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onNext(3);
                emitter.onComplete();
            }
        });

        /**
         * just
         */
        Observable observable1 = Observable.just("a", "b", "c", "sss");
        String[] words = {"A", "B", "C"};
        /**
         * fromArray
         */
        Observable observable2 = Observable.fromArray(words);


        //观察者  实现接口方式
        Observer<Integer> observer = new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Integer value) {
                Log.e("ssssss",value+"==========");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };
        observable.subscribe(observer);
        //观察者  实现抽象类方式

        /**
         * // 相同点：二者基本使用方式完全一致（实质上，在RxJava的 subscribe 过程中，Observer总是会先被转换成Subscriber再使用）
         // 不同点：Subscriber抽象类对 Observer 接口进行了扩展，新增了两个方法：
         // 1. onStart()：在还未响应事件前调用，用于做一些初始化工作
         // 2. unsubscribe()：用于取消订阅。在该方法被调用后，观察者将不再接收 & 响应事件
         // 调用该方法前，先使用 isUnsubscribed() 判断状态，确定被观察者Observable是否还持有观察者Subscriber的引用，如果引用不能及时释放，就会出现内存泄露
         */
        Subscriber<String> subscriber = new Subscriber<String>() {
            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(String s) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        };

        //定時器
//        Observable.interval(1000,1000, TimeUnit.MILLISECONDS)
//                .subscribe(aLong -> {
//                    Log.e("ssss",aLong+"===============");
//                });
        /**
         * interval
         */
        Observable.interval(3, TimeUnit.SECONDS).subscribe(System.out::print);
        // 流
        int max=Stream.of(1,2,3,4)
                .mapToInt(Integer->Integer)
                .max().getAsInt();


        File[] folders = new File[6];
        Observable.fromArray(folders)
                .flatMap(new Function<File, ObservableSource<File>>() {
                    @Override
                    public ObservableSource<File> apply(File file) throws Exception {
                        return Observable.fromArray(file.listFiles());
                    }
                })
                .filter(file->file.getName().endsWith("png"))
                .map(new Function<File, Bitmap>() {

                    @Override
                    public Bitmap apply(File file) throws Exception {
                        return getBitmapFromFile(file);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmap -> {
                    //todo
                });
    }

    public Bitmap getBitmapFromFile(File file){
        return null;
    }
}
