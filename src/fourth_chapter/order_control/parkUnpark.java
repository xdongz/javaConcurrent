/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */
package fourth_chapter.order_control;

import java.util.concurrent.locks.LockSupport;

//本类利用LockSupport中的park unpark方法去实现循环打印abc
public class parkUnpark {

  static Thread t1;
  static Thread t2;
  static Thread t3;

  public static void main(String[] args) {
    PrintChar3 printChar3 = new PrintChar3(5);
    t1 = new Thread(() -> {
      printChar3.print("a", t2);
    });

    t2 = new Thread(() -> {
      printChar3.print("b", t3);
    });

    t3 = new Thread(() -> {
      printChar3.print("c", t1);
    });

    t1.start();
    t2.start();
    t3.start();

    LockSupport.unpark(t1);
  }
}

class PrintChar3 {
  private final int loopNumber;

  public PrintChar3(int loopNumber) {
    this.loopNumber = loopNumber;
  }

  //打印当前字符，并且唤醒下一个线程
  public void print(String str, Thread next) {
    for (int i = 0; i < loopNumber; i ++) {
      LockSupport.park();
      System.out.println(str);
      LockSupport.unpark(next);
    }
  }
}
