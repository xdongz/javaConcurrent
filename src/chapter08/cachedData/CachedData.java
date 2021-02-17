/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */
package chapter08.cachedData;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

public class CachedData {

  Object data;
  //是否有效，如果失效，需重新计算 data
  volatile boolean cacheValid;
  final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

  public void processData() {
    rw.readLock().lock();
    if (!cacheValid) {
      //获取写锁前必须释放读锁
      rw.readLock().unlock();
      rw.writeLock().lock();
      try {
        //判断是否有其他线程之前获取了读锁并且修改了缓存数据
        if (!cacheValid) {
          System.out.println("修改缓存数据...");
          cacheValid = true;
        }
        //降级为读锁，释放写锁，使其他线程可以读取数据
        rw.readLock().lock();
      } finally {
        rw.writeLock().unlock();
      }
    }
    try {
      use(data);
    } finally {
      rw.readLock().unlock();
    }
  }

  public void use(Object data) {

  }

}
