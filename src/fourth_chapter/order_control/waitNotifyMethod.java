/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */
package fourth_chapter.order_control;

//本类是利用synchronized中的wait notify 方法交替打印abc
public class waitNotifyMethod {

  public static void main(String[] args) {

    PrintChar printChar = new PrintChar(1, 5);

    new Thread(() -> {
      printChar.print("a", 1, 2);
    }, "t1").start();

    new Thread(() -> {
      printChar.print("b", 2, 3);
    }, "t2").start();

    new Thread(() -> {
      printChar.print("c", 3, 1);
    }, "t3").start();

  }
}

//首先创建一个公共的类
//         要打印的字母，   当前的标记，     下一个标记
//  线程1      a              1              2
//  线程2      b              2              3
//  线程3      c              3              1
class PrintChar {

  private int flag;
  //设置循环打印几次
  private final int loopNumber;

  public PrintChar(int flag, int loopNumber) {
    this.flag = flag;
    this.loopNumber = loopNumber;
  }

  public void print(String str, int expectedFlag, int nextFlag) {
    for (int i = 0; i < loopNumber; i ++) {
      synchronized (this) {
        //当flag的值和期望的flag的值不一样时就等待
        while (expectedFlag != flag) {
          try {
            this.wait();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        //如果相等，就说明可以打印
        System.out.println(str);
        //打印完后设置下一个标记
        flag = nextFlag;
        this.notifyAll();
      }
    }
  }
}

