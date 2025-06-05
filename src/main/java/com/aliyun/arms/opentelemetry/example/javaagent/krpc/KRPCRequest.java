package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import io.opentelemetry.context.Context;
import krpc.rpc.core.RpcClosure;
import krpc.rpc.core.ServiceMetas;
import krpc.rpc.core.proto.RpcMeta;

public class KRPCRequest {
  private RpcMeta rpcMeta;
  private RpcClosure rpcClosure;

  private  final ServiceMetas serviceMetas;
  private Context context;
  private final int serviceId;
  private final int messageId;

  //服务端调这个方法
  public KRPCRequest(RpcMeta rpcMeta, ServiceMetas serviceMetas) {
    this.rpcMeta = rpcMeta;
    this.serviceMetas = serviceMetas;
    this.serviceId = rpcMeta.getServiceId();
    this.messageId = rpcMeta.getMsgId();
  }

  //客户端端调这个方法
  public KRPCRequest(RpcClosure rpcClosure, ServiceMetas serviceMetas) {
    this.rpcClosure = rpcClosure;
    this.serviceMetas = serviceMetas;
    this.serviceId = rpcClosure.getCtx().getMeta().getServiceId();
    this.messageId = rpcClosure.getCtx().getMeta().getMsgId();
  }


  public String getName() {
    return serviceMetas.getName(serviceId, messageId);
  }

  public String getServiceName() {
    return serviceMetas.getServiceName(serviceId);
  }

  public RpcMeta getRpcMeta() {
    return rpcMeta;
  }

  public RpcClosure getRpcClosure() {
    return rpcClosure;
  }

  public Context getContext() {
    return context;
  }

  public void setContext(Context context) {
    this.context = context;
  }
}
