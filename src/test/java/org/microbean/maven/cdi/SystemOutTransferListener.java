/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2017 MicroBean.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.maven.cdi;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;

@ApplicationScoped
public class SystemOutTransferListener extends AbstractTransferListener {

  public SystemOutTransferListener() {
    super();
  }
  
  @Override
  public void transferInitiated(final TransferEvent event) {
    System.out.println("*** transfer initiated: " + event);
  }
  
  @Override
  public void transferStarted(final TransferEvent event) {
    System.out.println("*** transfer started: " + event);
  }
  
  @Override
  public void transferProgressed(final TransferEvent event) {
    System.out.println("*** transfer progressed: " + event);
  }
  
  @Override
  public void transferSucceeded(final TransferEvent event) {
    System.out.println("*** transfer succeeded: " + event);
  }
  
  @Override
  public void transferCorrupted(final TransferEvent event) {
    System.out.println("*** transfer corrupted: " + event);
  }
  
  @Override
  public void transferFailed(final TransferEvent event) {
    System.out.println("*** transfer failed: " + event);
  }
  
}
