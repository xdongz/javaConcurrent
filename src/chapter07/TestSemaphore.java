package chapter07;/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * 本类是将之前的连接池改为用semaphore来控制线程数量而不用wait/notify
 */
public class TestSemaphore {

  public static void main(String[] args) {
    TestPool pool = new TestPool(2);

    for (int i = 0; i < 5; i++) {
      new Thread(() -> {
        Connection conn = pool.borrowConnection();
        try {
          Thread.sleep(new Random().nextInt(1000));
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        pool.freeConnection(conn);
      }).start();
    }
  }



}

class TestPool {

  //连接池的大小
  private final int poolSize;

  //连接池
  private Connection[] connection;

  //连接的状态，因为需要更改，所以用AtomicIntegerArray
  //0表示空闲，1表示占用
  private AtomicIntegerArray state;

  private Semaphore semaphore;

  public TestPool(int poolSize) {
    this.poolSize = poolSize;
    this.connection = new Connection[poolSize];
    this.state = new AtomicIntegerArray(poolSize);
    //让许可数与线程数一致
    this.semaphore = new Semaphore(poolSize);

    for (int i = 0; i < poolSize; i++) {
      connection[i] = new TestMockConnection("连接 " + (i+1));
    }
  }

  public Connection borrowConnection() {

    //获取许可
    try {
      semaphore.acquire(); //没有许可的线程在此等待
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    for (int i = 0; i < poolSize; i++) {
      //如果线程是空闲的，就borrow
      if (state.get(i) == 0) {
        //判断是否能修改成功
        if (state.compareAndSet(i, 0, 1)) {
          System.out.println("获取连接 "  + connection[i]);
          return connection[i];
        }
      }
    }
    return null;
  }

  //释放连接
  public void freeConnection(Connection conn) {
    for (int i = 0; i < poolSize; i++) {
      //释放某个连接，就是把它的状态改为0
      if (connection[i] == conn) {
        state.set(i, 0);
        System.out.println("归还连接 " + conn);
        //释放许可
        semaphore.release();
        break;
      }
    }
  }

}


//模拟connection, don't care
class TestMockConnection implements Connection {

  private String name;

  public TestMockConnection(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "TestMockConnection{" +
        "name='" + name + '\'' +
        '}';
  }

  @Override
  public Statement createStatement() throws SQLException {
    return null;
  }

  @Override
  public PreparedStatement prepareStatement(String sql) throws SQLException {
    return null;
  }

  @Override
  public CallableStatement prepareCall(String sql) throws SQLException {
    return null;
  }

  @Override
  public String nativeSQL(String sql) throws SQLException {
    return null;
  }

  @Override
  public void setAutoCommit(boolean autoCommit) throws SQLException {

  }

  @Override
  public boolean getAutoCommit() throws SQLException {
    return false;
  }

  @Override
  public void commit() throws SQLException {

  }

  @Override
  public void rollback() throws SQLException {

  }

  @Override
  public void close() throws SQLException {

  }

  @Override
  public boolean isClosed() throws SQLException {
    return false;
  }

  @Override
  public DatabaseMetaData getMetaData() throws SQLException {
    return null;
  }

  @Override
  public void setReadOnly(boolean readOnly) throws SQLException {

  }

  @Override
  public boolean isReadOnly() throws SQLException {
    return false;
  }

  @Override
  public void setCatalog(String catalog) throws SQLException {

  }

  @Override
  public String getCatalog() throws SQLException {
    return null;
  }

  @Override
  public void setTransactionIsolation(int level) throws SQLException {

  }

  @Override
  public int getTransactionIsolation() throws SQLException {
    return 0;
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    return null;
  }

  @Override
  public void clearWarnings() throws SQLException {

  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency)
      throws SQLException {
    return null;
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
      throws SQLException {
    return null;
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
      throws SQLException {
    return null;
  }

  @Override
  public Map<String, Class<?>> getTypeMap() throws SQLException {
    return null;
  }

  @Override
  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {

  }

  @Override
  public void setHoldability(int holdability) throws SQLException {

  }

  @Override
  public int getHoldability() throws SQLException {
    return 0;
  }

  @Override
  public Savepoint setSavepoint() throws SQLException {
    return null;
  }

  @Override
  public Savepoint setSavepoint(String name) throws SQLException {
    return null;
  }

  @Override
  public void rollback(Savepoint savepoint) throws SQLException {

  }

  @Override
  public void releaseSavepoint(Savepoint savepoint) throws SQLException {

  }

  @Override
  public Statement createStatement(int resultSetType, int resultSetConcurrency,
      int resultSetHoldability) throws SQLException {
    return null;
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
      int resultSetHoldability) throws SQLException {
    return null;
  }

  @Override
  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
      int resultSetHoldability) throws SQLException {
    return null;
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
    return null;
  }

  @Override
  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
    return null;
  }

  @Override
  public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
    return null;
  }

  @Override
  public Clob createClob() throws SQLException {
    return null;
  }

  @Override
  public Blob createBlob() throws SQLException {
    return null;
  }

  @Override
  public NClob createNClob() throws SQLException {
    return null;
  }

  @Override
  public SQLXML createSQLXML() throws SQLException {
    return null;
  }

  @Override
  public boolean isValid(int timeout) throws SQLException {
    return false;
  }

  @Override
  public void setClientInfo(String name, String value) throws SQLClientInfoException {

  }

  @Override
  public void setClientInfo(Properties properties) throws SQLClientInfoException {

  }

  @Override
  public String getClientInfo(String name) throws SQLException {
    return null;
  }

  @Override
  public Properties getClientInfo() throws SQLException {
    return null;
  }

  @Override
  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    return null;
  }

  @Override
  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    return null;
  }

  @Override
  public void setSchema(String schema) throws SQLException {

  }

  @Override
  public String getSchema() throws SQLException {
    return null;
  }

  @Override
  public void abort(Executor executor) throws SQLException {

  }

  @Override
  public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {

  }

  @Override
  public int getNetworkTimeout() throws SQLException {
    return 0;
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return null;
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }
}
