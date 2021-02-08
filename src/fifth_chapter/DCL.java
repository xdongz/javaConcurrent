/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */
package fifth_chapter;

//DCL 全称是double check locking
//为了保证懒汉式的单例模式的多线程安全
public class DCL {

  //instance变量前面需要加violatile关键字
  //是为了防止instance = new DCL()这句指令的重排序
  //有可能因为重排序，而导致会先执行赋值操作，然后再调用构造函数
  //那这样其他线程拿到的instance可能是未经构造函数处理的实例
  //而加了violatile关键字可以防止重排序
  private static volatile DCL instance = null;

  private DCL() {
  }

  public static DCL getInstance() {

    //double-check, 这样当对象实例化后，就可以直接返回instance
    //而不是每次都需要等锁
    if (instance != null) {
      return instance;
    }
    synchronized (DCL.class) {
      //这一次的判断是为了防止多线程访问时，重复创建对象
      if (instance != null) {
        return instance;
      }

      instance = new DCL();
      return instance;
    }
  }
}
