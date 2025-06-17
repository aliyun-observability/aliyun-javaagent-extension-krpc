package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.instrumentation.api.instrumenter.OperationMetrics;
import io.opentelemetry.sdk.trace.ReadableSpan;

import java.util.logging.Level;
import java.util.logging.Logger;

public class KRPCMetrics extends CommonServiceMetrics implements OperationListener {

  private static final ContextKey<ArmsRpcServerMetricsState> ARMS_RPC_SERVER_METRICS_CONTEXT_KEY =
          ContextKey.named("arms-rpc-server-metrics-state");

  private static final Logger logger = Logger.getLogger(KRPCMetrics.class.getName());

  public KRPCMetrics(Meter meter) {
    super(meter);
  }

  public static OperationMetrics get() {
    return KRPCMetrics::new;
  }

  @Override
  public Context onStart(Context context, Attributes startAttributes, long startNanos) {
    return context.with(ARMS_RPC_SERVER_METRICS_CONTEXT_KEY,
            new ArmsRpcServerMetricsState(startNanos));
  }

  @Override
  public void onEnd(Context context, Attributes endAttributes, long endNanos) {
    ArmsRpcServerMetricsState state = context.get(ARMS_RPC_SERVER_METRICS_CONTEXT_KEY);
    if (state == null) {
      logger.log(
              Level.FINE,
              "No state present when ending context {0}. Cannot record Rpc request metrics.",
              context);
      return;
    }
    // 所有的metrics attributes都是endAttributes，不用append start attributes了
    Span span = LocalRootSpan.fromContext(context);

    Attributes allAttributes = endAttributes;
    long duration = (endNanos - state.startTimeNanos) / NANOS_PER_MS;

    String rpc = getSpanName(span);
    if (rpc == null) {
      return;
    }
    allAttributes = allAttributes.toBuilder()
            .put(AttributeKey.stringKey("callType"), "krpc")
            .put(AttributeKey.stringKey("callKind"), "rpc")
            .put(AttributeKey.longKey("rpcType"), 102L)
            .put("rpc", rpc).build();
    recordNormalMetrics(context, duration, (rt) -> rt > 500, allAttributes);
  }

  public static String getSpanName(Span span) {
    try{
      return (String)span.getClass().getDeclaredMethod("getName").invoke(span);
    }catch (Throwable t){
      return null;
    }
  }

  static class ArmsRpcServerMetricsState {


    private final long startTimeNanos;

    public ArmsRpcServerMetricsState(long startTimeNanos) {
      this.startTimeNanos = startTimeNanos;
    }

  }
}
