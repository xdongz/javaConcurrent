/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */
package chapter08.diy_thread_pool;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPoolTest {

  public static void main(String[] args) {
    ThreadPool pool = new ThreadPool(1, 1000, TimeUnit.MILLISECONDS, 1, (queue, task) -> {
      //实现reject方法
      //1. 阻塞等待
      //queue.put(task);
      //2. 超时等待
      //queue.offer(task, 1500, TimeUnit.MILLISECONDS);
      //3. 放弃
      //System.out.println("放弃 " + task);
      //4. 抛出异常
      //抛出异常与放弃的区别是： 如果在执行任务2的时候抛出了异常，那么根本不会执行到任务3；
      //但如果是放弃的话，还是能打印出来任务3被放弃了
      //throw new RuntimeException("任务执行失败 " + task);
      //5. 调用者自己执行
      //这样调用，主线程会运行这个run方法
      task.run();
    });

    for (int i = 0; i < 4; i++) {
      int j = i;
      pool.execute(() -> {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        //每个任务具体的执行过程
        System.out.println("任务 " + j);
      });
    }
  }

}

//拒绝策略，函数式接口
@FunctionalInterface
interface RejectPolicy<T> {
  public void reject(BlockingQueue<T> queue, T task);
}

//线程池，包含两个部分：线程集合和阻塞队列
class ThreadPool {

  //阻塞队列
  private BlockingQueue<Runnable> taskQueue;

  //线程集合
  private HashSet<Worker> workers = new HashSet<>();

  //核心线程数，线程池里最多只能有这么多线程
  private int coreSize;

  //阻塞队列的最大容量
  private int queueSize;

  //超时时间，如果超过一定时间线程还没有任务执行就销毁线程
  private long timeout;

  private TimeUnit timeUnit;

  private RejectPolicy<Runnable> rejectPolicy;

  public ThreadPool(int coreSize, long timeout, TimeUnit timeUnit,
      int queueSize, RejectPolicy<Runnable> rejectPolicy) {
    this.coreSize = coreSize;
    this.timeout = timeout;
    this.timeUnit = timeUnit;
    this.taskQueue = new BlockingQueue<>(queueSize);
    this.rejectPolicy = rejectPolicy;
  }

  class Worker extends Thread{

    private Runnable task;

    public Worker(Runnable task) {
      this.task = task;
    }

    //执行任务
    @Override
    public void run() {

      //1.当task不为空，直接执行任务
      //2.当执行完任务后，从queue里获取任务执行
      while (task != null || (task = taskQueue.poll(timeout, timeUnit)) != null) {
        try {
          System.out.println("正在执行... " + task);
          task.run();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          task = null;
        }
      }

      //执行完任务之后把该线程从线程池中删掉
      synchronized (workers) {
        System.out.println("Worker 被移除了 " + this);
        workers.remove(this);
      }
    }

  }

  // 执行任务
  public void execute(Runnable task) {
    //当任务数没有超过线程核心数时，直接交给Woker对象执行任务
    //如果任务数超过了线程核心数时，就把任务加入到阻塞队列中
    //注意要保证workers集合的线程安全
    synchronized (workers) {
      if (workers.size() < coreSize) {
        Worker worker = new Worker(task);
        System.out.println("新增 worker " + worker + task);
        workers.add(worker);
        worker.start();
      } else {
        //taskQueue.put(task);
        //当线程都被占用了之后，对于新的task其实有很多处理方法
        //比如：1.阻塞添加 2. 超时添加 3. 调用者放弃 4. 调用者抛出异常 5. 调用者自己执行 等等
        //所以在这里我们不妨把具体的执行过程抽象成一个接口，让调用者自己去实现。
        //最好还是在taskQueue中实现，因为有锁
        taskQueue.tryPut(rejectPolicy, task);
      }
    }
  }
}


//阻塞队列
class BlockingQueue<T> {

  //任务队列
  private Deque<T> queue = new ArrayDeque<>();

  //锁
  private ReentrantLock lock = new ReentrantLock();

  //条件变量
  private Condition fullWaitSet = lock.newCondition();
  private Condition emptyWaitSet = lock.newCondition();

  //队列最大容量
  private int capcity;

  BlockingQueue(int capcity) {
    this.capcity = capcity;
  }

  //从队列中拿任务执行，阻塞获取
  public T take() {
    lock.lock();
    try {
      //如果队列中没有任务了，那么就等待
      while (queue.isEmpty()) {
        try {
          emptyWaitSet.await();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      //如果队列中有任务了，就取走任务
      T t = queue.removeFirst();
      //唤醒在fullWaitSet中等待的线程，告诉他们非满，可以往队列中加入任务
      fullWaitSet.signal();
      return t;
    } finally {
      lock.unlock();
    }
  }

  //take增强，增加了超时时间
  public T poll(long timeout, TimeUnit timeUnit) {
    lock.lock();
    try {
      //将timeout的单位转化为纳秒
      long nanos = timeUnit.toNanos(timeout);
      //如果队列中没有任务了，那么就等待
      while (queue.isEmpty()) {
        try {
          if (nanos <= 0) {
            return null;
          }
          //返回的是剩余的时间
          nanos = emptyWaitSet.awaitNanos(nanos);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      //如果队列中有任务了，就取走任务
      T t = queue.removeFirst();
      //唤醒在fullWaitSet中等待的线程，告诉他们非满，可以往队列中加入任务
      fullWaitSet.signal();
      return t;
    } finally {
      lock.unlock();
    }
  }

  //把任务加入队列，阻塞添加
  public void put(T element) {
    lock.lock();
    try {
      while (queue.size() == capcity) {
        try {
          System.out.println("等待加入任务队列 " + element);
          fullWaitSet.await();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      //如果队列没有满，那么就把任务放进队列
      queue.addLast(element);
      System.out.println("加入任务队列 " + element);
      //唤醒emptyWaitSet中等待的线程，告诉他们队列非空，可以移除任务了
      emptyWaitSet.signal();
    } finally {
      lock.unlock();
    }
  }

  //带超时的添加
  public boolean offer(T element, long timeout, TimeUnit timeUnit) {
    lock.lock();
    try {
      long nanos = timeUnit.toNanos(timeout);
      while (queue.size() == capcity) {
        try {
          if (nanos <= 0) {
            return false;
          }
          System.out.println("等待加入任务队列 " + element);
          nanos = fullWaitSet.awaitNanos(nanos);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      //如果队列没有满，那么就把任务放进队列
      queue.addLast(element);
      System.out.println("加入任务队列 " + element);
      //唤醒emptyWaitSet中等待的线程，告诉他们队列非空，可以移除任务了
      emptyWaitSet.signal();
      return true;
    } finally {
      lock.unlock();
    }
  }

  //获取队列的大小
  public int size() {
    lock.lock();
    try {
      return queue.size();
    } finally {
      lock.unlock();
    }
  }

  public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
    lock.lock();
    try {
      //判断队列是否已满
      if (queue.size() == capcity) {
        //具体执行什么方式，由rejectPolicy决定
        rejectPolicy.reject(this, task);
      } else {
        //未满
        queue.addLast(task);
        System.out.println("加入任务队列 " + task);
        //唤醒emptyWaitSet中等待的线程，告诉他们队列非空，可以移除任务了
        emptyWaitSet.signal();
      }
    } finally {
      lock.unlock();
    }
  }
}


