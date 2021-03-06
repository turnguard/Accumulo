package org.apache.accumulo.core.data;

/**
 * Copyright 2007 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.apache.accumulo.core.util.ByteBufferUtil.toBytes;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.accumulo.core.Constants;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.log4j.Logger;

/**
 * A byte sequence that is usable as a key or value. Based on {@link org.apache.hadoop.io.BytesWritable} only this class is NOT resizable and DOES NOT
 * distinguish between the size of the sequence and the current capacity as {@link org.apache.hadoop.io.BytesWritable} does. Hence its comparatively
 * 'immutable'.
 */
public class Value implements WritableComparable<Object> {
  protected byte[] value;
  private static final Logger log = Logger.getLogger(Value.class);
  
  /**
   * Create a zero-size sequence.
   */
  public Value() {
    super();
  }
  
  /**
   * Create a Value using the byte array as the initial value.
   * 
   * @param bytes
   *          This array becomes the backing storage for the object.
   */
  
  public Value(byte[] bytes) {
    this(bytes, false);
  }
  
  public Value(ByteBuffer bytes) {
    this(toBytes(bytes), false);
  }
  
  public Value(ByteBuffer bytes, boolean copy) {
    this(toBytes(bytes), copy);
  }
  
  public Value(byte[] bytes, boolean copy) {
    if (!copy) {
      this.value = bytes;
    } else {
      this.value = new byte[bytes.length];
      System.arraycopy(bytes, 0, this.value, 0, bytes.length);
    }
    
  }
  
  /**
   * Set the new Value to a copy of the contents of the passed <code>ibw</code>.
   * 
   * @param ibw
   *          the value to set this Value to.
   */
  public Value(final Value ibw) {
    this(ibw.get(), 0, ibw.getSize());
  }
  
  /**
   * Set the value to a copy of the given byte range
   * 
   * @param newData
   *          the new values to copy in
   * @param offset
   *          the offset in newData to start at
   * @param length
   *          the number of bytes to copy
   */
  public Value(final byte[] newData, final int offset, final int length) {
    this.value = new byte[length];
    System.arraycopy(newData, offset, this.value, 0, length);
  }
  
  /**
   * Get the data from the BytesWritable.
   * 
   * @return The data is only valid between 0 and getSize() - 1.
   */
  public byte[] get() {
    if (this.value == null) {
      throw new IllegalStateException("Uninitialized. Null constructor " + "called w/o accompanying readFields invocation");
    }
    return this.value;
  }
  
  /**
   * @param b
   *          Use passed bytes as backing array for this instance.
   */
  public void set(final byte[] b) {
    this.value = b;
  }
  
  /**
   * 
   * @param b
   *          copy bytes
   */
  public void copy(byte[] b) {
    this.value = new byte[b.length];
    System.arraycopy(b, 0, this.value, 0, b.length);
  }
  
  /**
   * @return the current size of the buffer.
   */
  public int getSize() {
    if (this.value == null) {
      throw new IllegalStateException("Uninitialized. Null constructor " + "called w/o accompanying readFields invocation");
    }
    return this.value.length;
  }
  
  /** {@inheritDoc} */
  public void readFields(final DataInput in) throws IOException {
    this.value = new byte[in.readInt()];
    in.readFully(this.value, 0, this.value.length);
  }
  
  /** {@inheritDoc} */
  public void write(final DataOutput out) throws IOException {
    out.writeInt(this.value.length);
    out.write(this.value, 0, this.value.length);
  }
  
  // Below methods copied from BytesWritable
  
  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return WritableComparator.hashBytes(value, this.value.length);
  }
  
  /**
   * Define the sort order of the BytesWritable.
   * 
   * @param right_obj
   *          The other bytes writable
   * @return Positive if left is bigger than right, 0 if they are equal, and negative if left is smaller than right.
   */
  public int compareTo(Object right_obj) {
    return compareTo(((Value) right_obj).get());
  }
  
  /**
   * Compares the bytes in this object to the specified byte array
   * 
   * @return Positive if left is bigger than right, 0 if they are equal, and negative if left is smaller than right.
   */
  public int compareTo(final byte[] that) {
    int diff = this.value.length - that.length;
    return (diff != 0) ? diff : WritableComparator.compareBytes(this.value, 0, this.value.length, that, 0, that.length);
  }
  
  /** {@inheritDoc} */
  @Override
  public boolean equals(Object right_obj) {
    if (right_obj instanceof byte[]) {
      return compareTo((byte[]) right_obj) == 0;
    }
    if (right_obj instanceof Value) {
      return compareTo(right_obj) == 0;
    }
    return false;
  }
  
  @Override
  public String toString() {
    try {
      return Value.bytesToString(this.value);
    } catch (UnsupportedEncodingException e) {
      log.error(e.toString());
      return null;
    }
  }
  
  /**
   * A Comparator optimized for Value.
   */
  public static class Comparator extends WritableComparator {
    private BytesWritable.Comparator comparator = new BytesWritable.Comparator();
    
    /** constructor */
    public Comparator() {
      super(Value.class);
    }
    
    /** {@inheritDoc} */
    @Override
    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
      return comparator.compare(b1, s1, l1, b2, s2, l2);
    }
  }
  
  static { // register this comparator
    WritableComparator.define(Value.class, new Comparator());
  }
  
  /**
	 */
  public static byte[][] toArray(final List<byte[]> array) {
    // List#toArray doesn't work on lists of byte [].
    byte[][] results = new byte[array.size()][];
    for (int i = 0; i < array.size(); i++) {
      results[i] = array.get(i);
    }
    return results;
  }
  
  /**
   * Convert a long value to a byte array
   * 
   * @deprecated since 1.4.1. There is no replacement.
   */
  public static byte[] longToBytes(long val) throws IOException {
    return getBytes(new LongWritable(val));
  }
  
  /**
   * Converts a byte array to a long value
   * 
   * @deprecated since 1.4.1. There is no replacement.
   */
  public static long bytesToLong(byte[] bytes) throws IOException {
    if (bytes == null || bytes.length == 0) {
      return -1L;
    }
    return ((LongWritable) getWritable(bytes, new LongWritable())).get();
  }
  
  /**
   * Converts a string to a byte array in a consistent manner.
   * 
   * @deprecated since 1.4.1. There is no replacement.
   */
  public static byte[] stringToBytes(String s) throws UnsupportedEncodingException {
    if (s == null) {
      throw new IllegalArgumentException("string cannot be null");
    }
    return s.getBytes(Constants.VALUE_ENCODING);
  }
  
  /**
   * Converts a byte array to a string in a consistent manner.
   * 
   * @deprecated since 1.4.1. There is no replacement.
   */
  public static String bytesToString(byte[] bytes) throws UnsupportedEncodingException {
    if (bytes == null || bytes.length == 0) {
      return "";
    }
    return new String(bytes, Constants.VALUE_ENCODING);
  }
  
  /**
   * @return The bytes of <code>w</code> gotten by running its {@link Writable#write(java.io.DataOutput)} method.
   * @see #getWritable(byte[], Writable)
   * 
   * @deprecated since 1.4.1. There is no replacement.
   */
  public static byte[] getBytes(final Writable w) throws IOException {
    if (w == null) {
      throw new IllegalArgumentException("Writable cannot be null");
    }
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(byteStream);
    try {
      w.write(out);
      out.close();
      out = null;
      return byteStream.toByteArray();
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
  
  /**
   * Set bytes into the passed Writable by calling its {@link Writable#readFields(java.io.DataInput)}.
   * 
   * @param w
   *          An empty Writable (usually made by calling the null-arg constructor).
   * @return The passed Writable after its readFields has been called fed by the passed <code>bytes</code> array or IllegalArgumentException if passed null or
   *         an empty <code>bytes</code> array.
   * 
   * @deprecated since 1.4.1. There is no replacement.
   */
  public static Writable getWritable(final byte[] bytes, final Writable w) throws IOException {
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("Can't build a writable with empty " + "bytes array");
    }
    if (w == null) {
      throw new IllegalArgumentException("Writable cannot be null");
    }
    DataInputBuffer in = new DataInputBuffer();
    try {
      in.reset(bytes, bytes.length);
      w.readFields(in);
      return w;
    } finally {
      in.close();
    }
  }
}
