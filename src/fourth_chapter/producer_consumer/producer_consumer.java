/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */
package fourth_chapter.producer_consumer;

import java.util.LinkedList;

//本类是利用synchronized实现生产者消费者模式
public class producer_consumer {

  public static void main(String[] args) {
    MessageQueue messageQueue = new MessageQueue(2);

    //生产者线程
    for (int i = 0; i < 3; i ++) {
      int id = i;
      new Thread(() -> {
        messageQueue.put(new Message(id, "值" + id));
      }).start();
    }


    //消费者线程
    new Thread(() -> {
      while (true) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        messageQueue.take();
      }
    }).start();
  }
}

//消息队列类。
//生产者调用put方法向队列中加入消息
//消费者调用take方法向队列中取走消息
class MessageQueue {
  private LinkedList<Message> list = new LinkedList();
  private int capacity;

  public MessageQueue(int capacity) {
    this.capacity = capacity;
  }

  public void put(Message message) {
    synchronized (this) {
      while (list.size() == capacity) {
        try {
          System.out.println("等待取走消息");
          this.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      list.addLast(message);
      System.out.println("加入消息" + message);
      this.notifyAll();
    }
  }

  public Message take() {
    synchronized (this) {
      //当list是空的就等待
      while (list.isEmpty()) {
        try {
          System.out.println("等待加入消息");
          this.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      Message message = list.removeFirst();
      System.out.println("取走消息" + message);
      this.notifyAll();
      return message;
    }
  }
}

class Message {
  private int id;
  private String mail;

  public Message(int id, String mail) {
    this.id = id;
    this.mail = mail;
  }

  @Override
  public String toString() {
    return "Message{" +
        "id=" + id +
        ", mail='" + mail + '\'' +
        '}';
  }
}
