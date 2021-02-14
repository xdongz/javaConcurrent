/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */
package chapter08.aqs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class TestAqs {

  public static void main(String[] args) {
    MyLock lock = new MyLock();
    new Thread(() -> {
      lock.lock();
      try {
        System.out.println("线程1 正在执行");
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } finally {
        System.out.println("线程1 解锁");
        lock.unlock();
      }
    }).start();

    new Thread(() -> {
      lock.lock();
      try {
        System.out.println("线程2 正在执行");
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } finally {
        System.out.println("线程2 解锁");
        lock.unlock();
      }
    }).start();
  }
}

//利用AQS实现一个不可重入锁
//不可重入锁就是如果该线程获得了一个锁但没有释放的话，第二次要获得锁时也会被挡住
class MyLock implements Lock {

  //独占锁，同步器
  class MySync extends AbstractQueuedSynchronizer {

    @Override
    protected boolean tryAcquire(int arg) {
      if (compareAndSetState(0 , 1)) {
        setExclusiveOwnerThread(Thread.currentThread());
        return true;
      }
      return false;
    }

    @Override
    protected boolean tryRelease(int arg) {
      setExclusiveOwnerThread(null);
      //注意：这里的setState要放在setExclusiveOwnerThread后面
      //因为state是个volatile的变量，volatile变量后面会有一个写屏障，保障它之前的变量都能在主存中更新至最新值
      setState(0);
      return true;
    }

    @Override //是否持有独占锁
    protected boolean isHeldExclusively() {
      return getState() == 1;
    }

    public Condition newCondition() {
      return new ConditionObject();
    }
  }

  MySync sync = new MySync();

  @Override
  public void lock() {
    sync.acquire(1);
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    sync.acquireInterruptibly(1);
  }

  @Override
  public boolean tryLock() {
    return sync.tryAcquire(1);
  }

  @Override
  public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
    return sync.tryAcquireNanos(1, unit.toNanos(time));
  }

  @Override
  public void unlock() {
    //release会唤醒正在等待的线程
    sync.release(1);
  }

  @Override
  public Condition newCondition() {
    return sync.newCondition();
  }
}
