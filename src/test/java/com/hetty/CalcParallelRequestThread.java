package com.hetty;

import java.util.concurrent.CountDownLatch;

import com.hetty.server.Hello;

public class CalcParallelRequestThread implements Runnable {

    private CountDownLatch signal;
    private CountDownLatch finish;
    private Hello hello;
    private int taskNumber = 0;

    public CalcParallelRequestThread(Hello hello, CountDownLatch signal, CountDownLatch finish, int taskNumber) {
        this.signal = signal;
        this.finish = finish;
        this.taskNumber = taskNumber;
        this.hello = hello;
    }

    public void run() {
        try {
            signal.await();

            System.out.println("calc add result:[" + hello.hello(""+taskNumber) + "]");

            finish.countDown();
        } catch (InterruptedException ex) {
        	ex.printStackTrace();
        	System.out.println(ex);
        }
    }
}