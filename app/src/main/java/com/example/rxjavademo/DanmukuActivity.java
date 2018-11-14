package com.example.rxjavademo;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;

public class DanmukuActivity extends AppCompatActivity {
    private DanmakuView mDanmakuView;
    private DanmakuContext mContext;
    private BaseDanmakuParser mParser;

    private final int MAX_DANMAKU_LINES = 8; //弹幕在屏幕显示的最大行数

    private ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor = null;
    private ConcurrentLinkedQueue<DanmakuMsg> mQueue = null; //所有的弹幕数据存取队列，在这里做线程的弹幕取和存
    private ArrayList<DanmakuMsg> danmakuLists = null;//每次请求最新的弹幕数据后缓存list

    private final int WHAT_GET_LIST_DATA = 0xffa01;
    private final int WHAT_DISPLAY_SINGLE_DANMAKU = 0xffa02;

    private final int BASE_TIME = 400;
    private final int BASE_TIME_ADD = 100;

    //标志文本弹幕的序列号
    //区别不同弹幕
    private static int danmakuTextMsgId = 0;

    private final int[] colors = {Color.RED, Color.YELLOW, Color.BLUE, Color.GREEN, Color.CYAN, Color.DKGRAY};

    private Handler mDanmakuHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case WHAT_GET_LIST_DATA:
                    mDanmakuHandler.removeMessages(WHAT_GET_LIST_DATA);

                    if (danmakuLists != null && !danmakuLists.isEmpty()) {
                        mQueue.addAll(danmakuLists);
                        danmakuLists.clear();

                        if (!mQueue.isEmpty())
                            mDanmakuHandler.sendEmptyMessage(WHAT_DISPLAY_SINGLE_DANMAKU);
                    }

                    break;

                case WHAT_DISPLAY_SINGLE_DANMAKU:
                    mDanmakuHandler.removeMessages(WHAT_DISPLAY_SINGLE_DANMAKU);
                    displayDanmaku();
                    break;
            }
        }
    };

    /**
     * 弹幕数据封装的类（bean）
     */
    private class DanmakuMsg {
        public String msg;
    }

    private void displayDanmaku() {
        boolean p = mDanmakuView.isPaused();
        //如果当前的弹幕由于Android生命周期的原因进入暂停状态，那么不应该不停的消耗弹幕数据
        //要知道，在这里发出一个handler消息，那么将会消费（删掉）ConcurrentLinkedQueue头部的数据
        if (!mQueue.isEmpty() && !p) {
            DanmakuMsg dm = mQueue.poll();
            if (!TextUtils.isEmpty(dm.msg)) {
                addDanmaku(dm.msg, true);
            }

            mDanmakuHandler.sendEmptyMessageDelayed(WHAT_DISPLAY_SINGLE_DANMAKU, (long) (Math.random() * BASE_TIME) + BASE_TIME_ADD);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_danmuku);

        danmakuLists = new ArrayList<>();
        mQueue = new ConcurrentLinkedQueue<>();


        initDanmaku();

        mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        GetDanmakuMessageTask mTask = new GetDanmakuMessageTask();
        //延迟0秒执行，每隔若干秒周期执行一次任务
        mScheduledThreadPoolExecutor.scheduleAtFixedRate(mTask, 0, 5, TimeUnit.SECONDS);

    }


    /**
     * 假设该线程任务模拟的就是从网络中取弹幕数据的耗时操作
     * 假设这些弹幕数据序列是有序的。
     */
    private class GetDanmakuMessageTask implements Runnable {
        @Override
        public void run() {
            danmakuLists.clear();

            int count = (int) (Math.random() * 50);
            for (int i = 0; i < count; i++) {
                DanmakuMsg message = new DanmakuMsg();
                message.msg = "弹幕:" + danmakuTextMsgId;
                danmakuLists.add(message);

                danmakuTextMsgId++;
            }

            if (!danmakuLists.isEmpty()) {
                Message msg = mDanmakuHandler.obtainMessage();
                msg.what = WHAT_GET_LIST_DATA;
                mDanmakuHandler.sendMessage(msg);
            }
        }
    }

    /**
     * 驱动弹幕显示机制重新运作起来
     */
    private void resumeDanmaku() {
        if (!mQueue.isEmpty())
            mDanmakuHandler.sendEmptyMessageDelayed(WHAT_DISPLAY_SINGLE_DANMAKU, (int) (Math.random() * BASE_TIME) + BASE_TIME_ADD);
    }

    private void clearDanmaku() {
        if (danmakuLists != null && !danmakuLists.isEmpty()) {
            danmakuLists.clear();
        }

        if (mQueue != null && !mQueue.isEmpty())
            mQueue.clear();

        mDanmakuView.clearDanmakusOnScreen();
        mDanmakuView.clear();
    }

    private void initDanmaku() {
        mContext = DanmakuContext.create();

        // 设置最大显示行数
        HashMap<Integer, Integer> maxLinesPair = new HashMap<>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, MAX_DANMAKU_LINES); // 滚动弹幕最大显示5行

        // 设置是否禁止重叠
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);

        //普通文本弹幕也描边设置样式
        //如果是图文混合编排编排，最后不要描边
        mContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 10) //描边的厚度
                .setDuplicateMergingEnabled(false)
                .setScrollSpeedFactor(1.2f) //弹幕的速度。注意！此值越小，速度越快！值越大，速度越慢。// by phil
                .setScaleTextSize(1.2f)  //缩放的值
//        .setCacheStuffer(new BackgroundCacheStuffer())  // 绘制背景使用BackgroundCacheStuffer
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair);

        mParser = new BaseDanmakuParser() {
            @Override
            protected IDanmakus parse() {
                return null;
            }
        };
        mDanmakuView.prepare(mParser, mContext);

        //mDanmakuView.showFPS(true);
        mDanmakuView.enableDanmakuDrawingCache(true);

        if (mDanmakuView != null) {
            mDanmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback() {
                @Override
                public void updateTimer(DanmakuTimer timer) {
                }

                @Override
                public void drawingFinished() {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {
                    //Log.d("弹幕文本", "danmakuShown text=" + danmaku.text);
                }

                @Override
                public void prepared() {
                    mDanmakuView.start();
                }
            });
        }
    }

    private void sendTextMessage() {
        addDanmaku("zhangphil@csdn: " + System.currentTimeMillis(), true);
    }

    private void addDanmaku(CharSequence cs, boolean islive) {
        BaseDanmaku danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku == null || mDanmakuView == null) {
            return;
        }

        danmaku.text = cs;
        danmaku.padding = 5;
        danmaku.priority = 0;  // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = islive;
        danmaku.setTime(mDanmakuView.getCurrentTime() + 1200);
        danmaku.textSize = 20f * (mParser.getDisplayer().getDensity() - 0.6f); //文本弹幕字体大小
        danmaku.textColor = getRandomColor(); //文本的颜色
        danmaku.textShadowColor = getRandomColor(); //文本弹幕描边的颜色
        //danmaku.underlineColor = Color.DKGRAY; //文本弹幕下划线的颜色
        danmaku.borderColor = getRandomColor(); //边框的颜色

        mDanmakuView.addDanmaku(danmaku);
    }
    private void closeGetDanmakuMessage() {
        if (mScheduledThreadPoolExecutor != null)
            mScheduledThreadPoolExecutor.shutdown();
    }
    /**
     * 从一系列颜色中随机选择一种颜色
     *
     * @return
     */
    private int getRandomColor() {
        int i = ((int) (Math.random() * 10)) % colors.length;
        return colors[i];
    }

}
