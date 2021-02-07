/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */
package fourth_chapter.guarded_suspension;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

//本类是用来实现保护性暂停模式。
//该模式是生产者和消费者线程一一对应（本例中是通过id号来对饮）
//如果生产者还没生产东西，相对应的消费者线程会等待。
public class guarded_suspension {

  public static void main(String[] args) throws InterruptedException {

    //消费者
    for (int i = 0; i < 3; i ++) {
      new people().start();
    }

    Thread.sleep(1000);
    //生产者
    for (int id : MailBox.getIds()) {
      new postman(id, "内容 " + id).start();
    }
  }
}

class people extends Thread {
  @Override
  public void run() {
    //收信
    //首先要创建邮筒
    GuardedObject guardedObject = MailBox.createGuardedObject();
    System.out.println("开始收信: " + guardedObject.getId());
    Object mail = guardedObject.get(5000);
    System.out.println("收到信: " + guardedObject.getId() + " " + mail);
  }
}

class postman extends Thread {
  private int id;
  private String mail;

  public postman(int id, String mail) {
    this.id = id;
    this.mail = mail;
  }

  @Override
  public void run() {
    //送信
    //首先要根据id获得邮筒
    GuardedObject guardedObject = MailBox.getGuardedObject(id);
    System.out.println("开始送信: " + guardedObject.getId());
    guardedObject.put(mail);
    System.out.println("送到信了: " + guardedObject.getId());
  }
}

//生产者和消费者中间过渡的一个类
//可以理解成存放着很多个邮筒，通过这些邮筒来实现生产者消费者一一对应的关系。
//这样就把生产者和消费者线程解耦开了
class MailBox {
  private static Map<Integer, GuardedObject> boxes = new Hashtable<>();
  private static int id = 1;
  //产生唯一的id，
  //通过这个id将邮递员和收件人一一对应起来
  public static int generateId() {
    return id ++;
  }

  //相当于为每个id创建一个唯一的邮筒，用来收发信件
  public static GuardedObject createGuardedObject() {
    GuardedObject go = new GuardedObject(generateId());

    boxes.put(go.getId(), go);
    return go;
  }

  //通过唯一的id号来获取中间类中的邮筒
  public static GuardedObject getGuardedObject(int id) {
    return boxes.remove(id);
  }

  public static Set<Integer> getIds() {
    return boxes.keySet();
  }

}

//可以理解成邮筒类
//生产者往对应的邮筒中加东西
//消费者往对应的邮筒中取东西
class GuardedObject {
  private int id;
  private Object response;

  public int getId() {
    return id;
  }

  public GuardedObject(int id) {
    this.id = id;
  }

  public Object get(long timeout) {
    synchronized (this) {
      long initTime = System.currentTimeMillis();
      long passedTime = 0;
      while (response == null) {
        long waitTime = timeout - passedTime;
        if (waitTime <= 0) {
          break;
        }
        try {
          this.wait(waitTime);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        passedTime = System.currentTimeMillis() - initTime;
      }
      this.notifyAll();
      return response;
    }
  }

  public void put(Object response) {
    synchronized (this) {
      this.response = response;
      this.notifyAll();
    }
  }
}
