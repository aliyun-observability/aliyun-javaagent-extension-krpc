package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import krpc.rpc.core.proto.RpcMeta;

public class KRPCResponse {
  private final Object response;

  public KRPCResponse(Object response) {
    this.response = response;
  }

  public Object getResponse() {
    return response;
  }
}
