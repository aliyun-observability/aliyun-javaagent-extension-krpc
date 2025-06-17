package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import krpc.rpc.core.RpcClosure;


public class KRPCSingleton {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.krpc";

  private static final Instrumenter<KRPCRequest, KRPCResponse> SERVER_INSTRUMENTER;

  private static final Instrumenter<KRPCRequest, KRPCResponse> CLIENT_INSTRUMENTER;

  private static final VirtualField<RpcClosure, KRPCRequest> FIELD1 = VirtualField.find(RpcClosure.class, KRPCRequest.class);

  static {

    SERVER_INSTRUMENTER =
            Instrumenter.<KRPCRequest, KRPCResponse>builder(
                            GlobalOpenTelemetry.get(),
                            INSTRUMENTATION_NAME,
                            new KRPCSpanNameExtractor())
                    .addAttributesExtractor(KrpcAttributesExtractor.INSTANCE)
                    .addOperationMetrics(KRPCMetrics::new)
                    .buildServerInstrumenter(KRPCHeaderGetter.INSTANCE);

    CLIENT_INSTRUMENTER =
            Instrumenter.<KRPCRequest, KRPCResponse>builder(
                            GlobalOpenTelemetry.get(),
                            INSTRUMENTATION_NAME,
                            new KRPCSpanNameExtractor())
                    .addAttributesExtractor(KrpcAttributesExtractor.INSTANCE)
                    .buildClientInstrumenter(KRPCHeaderSetter.INSTANCE);
  }

  public static Instrumenter<KRPCRequest, KRPCResponse> serverInstrumenter() {
    return SERVER_INSTRUMENTER;
  }

  public static Instrumenter<KRPCRequest, KRPCResponse> clientInstrumenter() {
    return CLIENT_INSTRUMENTER;
  }

  public static void storeRequestInRpcClosure(RpcClosure rpcClosure, KRPCRequest krpcRequest) {
    FIELD1.set(rpcClosure, krpcRequest);
  }

  public static KRPCRequest getKrpcRequestFromRpcClosure(RpcClosure rpcClosure) {
    return FIELD1.get(rpcClosure);
  }
}
