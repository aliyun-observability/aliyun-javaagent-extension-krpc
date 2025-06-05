/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import io.opentelemetry.context.propagation.TextMapSetter;
import krpc.rpc.core.RpcClosure;
import krpc.rpc.core.proto.RpcMeta;

enum KRPCHeaderSetter implements TextMapSetter<KRPCRequest> {
  INSTANCE;

  @Override
  public void set(KRPCRequest request, String key, String value) {
    if (key.contains("&") || value.contains("&")) {
      return;
    }
    RpcClosure rpcClosure = request.getRpcClosure();

    RpcMeta rpcMeta = rpcClosure.getCtx().getMeta();
    String original = rpcMeta.getTrace().getTags();
    if (original.length() == 0) {
      original = key + "=" + value;
    } else {
      original += "&" + key + "=" + value;
    }

    RpcMeta.Trace newTrace = rpcMeta.getTrace().toBuilder().setTags(original).build();
    rpcClosure.getCtx().setMeta(rpcMeta.toBuilder().setTrace(newTrace).build());
  }
}
