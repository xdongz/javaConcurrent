/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */
package fourth_chapter.order_control;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

//本类是利用ReentrantLock中的方法去实现三个线程循环打印abc
public class reentrantLock {

  public static void main(String[] args) throws InterruptedException {
    PrintChar2 printChar2 = new PrintChar2(5);

    //不同的线程在不同的休息室里等待
    Condition a = printChar2.newCondition();
    Condition b = printChar2.newCondition();
    Condition c = printChar2.newCondition();

    new Thread(() -> {
      printChar2.print("a", a, b);
    }).start();

    new Thread(() -> {
      printChar2.print("b", b, c);
    }).start();

    new Thread(() -> {
      printChar2.print("c", c, a);
    }).start();

    Thread.sleep(1000);
    printChar2.lock();
    try {
      //要先获取锁了才能signal
      a.signal();
    }finally {
      printChar2.unlock();
    }
  }
}

//首先这个类要继承ReentrantLock
class PrintChar2 extends ReentrantLock {

  int loopNumber;

  public PrintChar2(int loopNumber) {
    this.loopNumber = loopNumber;
  }

  public void print(String str, Condition current, Condition next) {
    for (int i = 0; i < loopNumber; i ++) {
      //相当于省略了前面的this
      lock();
      try {
        current.await();
        System.out.println(str);
        next.signal();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        unlock();
      }
    }
  }
}
