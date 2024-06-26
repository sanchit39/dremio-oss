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
package com.dremio.exec.rpc;

import com.dremio.exec.proto.GeneralRPCProtos.RpcMode;
import com.google.common.collect.Lists;
import com.google.protobuf.Internal.EnumLite;
import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;

public class OutboundRpcMessage extends RpcMessage {
  static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(OutboundRpcMessage.class);

  final MessageLite pBody;
  final ByteBuf[] dBodies;

  public OutboundRpcMessage(
      RpcMode mode, EnumLite rpcType, int coordinationId, MessageLite pBody, ByteBuf... dBodies) {
    this(mode, rpcType.getNumber(), coordinationId, pBody, dBodies);
  }

  OutboundRpcMessage(
      RpcMode mode, int rpcTypeNumber, int coordinationId, MessageLite pBody, ByteBuf... dBodies) {
    super(mode, rpcTypeNumber, coordinationId);
    this.pBody = pBody;

    // Netty doesn't traditionally release the reference on an unreadable buffer.  However, we need
    // to so that if we send a empty or unwritable buffer, we still release.  otherwise we get weird
    // memory leaks when sending empty vectors.
    List<ByteBuf> bufs = Lists.newArrayList();
    for (ByteBuf d : dBodies) {
      if (d.readableBytes() == 0) {
        d.release();
      } else {
        bufs.add(d);
      }
    }

    this.dBodies = bufs.toArray(new ByteBuf[bufs.size()]);
  }

  public MessageLite getPBody() {
    return pBody;
  }

  public ByteBuf[] getDBodies() {
    return dBodies;
  }

  @Override
  public int getBodySize() {
    int len = pBody.getSerializedSize();
    len += RpcEncoder.getRawVarintSize(len);
    len += getRawBodySize();
    return len;
  }

  public int getRawBodySize() {
    int len = 0;

    for (int i = 0; i < dBodies.length; i++) {
      if (RpcConstants.EXTRA_DEBUGGING) {
        logger.debug(
            "Reader Index {}, Writer Index {}", dBodies[i].readerIndex(), dBodies[i].writerIndex());
      }
      len += dBodies[i].readableBytes();
    }
    return len;
  }

  @Override
  public String toString() {
    return "OutboundRpcMessage [pBody="
        + pBody
        + ", mode="
        + mode
        + ", rpcType="
        + rpcType
        + ", coordinationId="
        + coordinationId
        + ", dBodies="
        + Arrays.toString(dBodies)
        + "]";
  }

  @Override
  void release() {
    for (ByteBuf b : dBodies) {
      b.release();
    }
  }
}
