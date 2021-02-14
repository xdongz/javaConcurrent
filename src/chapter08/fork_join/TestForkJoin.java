/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */
package chapter08.fork_join;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

//执行1+2+3...+n
public class TestForkJoin {

  public static void main(String[] args) {
    //默认线程等于核心数
    ForkJoinPool pool = new ForkJoinPool();
    System.out.println(pool.invoke(new Addtask(1, 5)));
  }

}

class Addtask extends RecursiveTask<Integer> {

  private int result;
  private int begin;
  private int end;

  public Addtask(int begin, int end) {
    this.begin = begin;
    this.end = end;
  }

  @Override
  protected Integer compute() {
    if (begin == end) {
      return begin;
    }

    if (end - begin == 1) {
      return (begin + end);
    }

    //体现了分治的思想，把任务分解成一个一个小任务
    //也像递归
    int mid = (begin + end) / 2;
    Addtask t1 = new Addtask(begin, mid);
    Addtask t2 = new Addtask(mid + 1, end);
    t1.fork();
    t2.fork();
    result = t1.join() + t2.join();
    return result;
  }
}
