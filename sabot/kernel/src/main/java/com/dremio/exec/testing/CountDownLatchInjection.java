/*
 * Copyright (C) 2017-2019 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.exec.testing;

/**
 * This class is used internally for tracking injected countdown latches. These latches are
 * specified via {@link com.dremio.exec.ExecConstants#NODE_CONTROL_INJECTIONS} session option.
 *
 * <p>This injection is useful in the case where a thread spawns multiple threads. The parent thread
 * initializes the latch with the expected number of countdown and awaits. The child threads count
 * down on the same latch (same site class and same descriptor), and once there are enough, the
 * parent thread continues.
 */
public interface CountDownLatchInjection {

  /**
   * Initializes the underlying latch
   *
   * @param count the number of times {@link #countDown} must be invoke before threads can pass
   *     through {@link #await}
   */
  void initialize(final int count);

  /**
   * Causes the current thread to wait until the latch has counted down to zero, unless the thread
   * is {@link Thread#interrupt interrupted}.
   */
  void await() throws InterruptedException;

  /**
   * Await without interruption. In the case of interruption, log a warning and continue to wait.
   */
  void awaitUninterruptibly();

  /** Decrements the count of the latch, releasing all waiting threads if the count reaches zero. */
  void countDown();

  /** Close the latch. */
  void close();
}
