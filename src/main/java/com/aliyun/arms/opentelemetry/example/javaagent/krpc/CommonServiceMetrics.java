package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.StatusData;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class CommonServiceMetrics {
  protected static final long NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);
  protected final LongCounter totalRequestCount;
  protected final LongCounter errorRequestCount;
  protected final LongCounter slowRequestCount;
  protected final LongCounter statusRequestCount;
  protected final LongCounter requestDuration;
  //protected final DoubleHistogram requestDuration;
  protected final DoubleHistogram requestQuantileDuration;

  protected final LongHistogram responseSize;
  protected final LongHistogram requestSize;
  protected final DoubleHistogram delayDuration;

  public CommonServiceMetrics(Meter meter) {
    totalRequestCount =
            meter
                    .counterBuilder("arms_rpc_requests_count")
                    .setUnit("{requests}")
                    .setDescription("The number of concurrent HTTP requests that are currently in-flight")
                    .build();
    errorRequestCount =
            meter
                    .counterBuilder("arms_rpc_requests_error_count")
                    .setUnit("{requests}")
                    .setDescription("The number of concurrent HTTP requests that are currently in-flight")
                    .build();
    slowRequestCount =
            meter
                    .counterBuilder("arms_rpc_requests_slow_count")
                    .setUnit("{requests}")
                    .setDescription("The number of concurrent HTTP requests that are currently in-flight")
                    .build();
    statusRequestCount =
            meter
                    .counterBuilder("arms_rpc_requests_by_status_count")
                    .setUnit("{requests}")
                    .setDescription("The number of concurrent HTTP requests that are currently in-flight")
                    .build();
//    requestDuration =
//        meter
//            .histogramBuilder("arms_rpc_requests_seconds")
//            .setUnit("ms")
//            .setDescription("The number of concurrent HTTP requests that are currently in-flight")
//            .build();

    requestDuration =
            meter
                    .counterBuilder("arms_rpc_requests_seconds")
                    .setUnit("{ms}")
                    .setDescription("The number of concurrent HTTP requests that are currently in-flight")
                    .build();

    requestQuantileDuration =
            meter
                    .histogramBuilder("arms_rpc_requests_latency_seconds")
                    .setUnit("{requests}")
                    .setDescription("The number of concurrent HTTP requests that are currently in-flight")
                    .build();
    responseSize =
            meter
                    .histogramBuilder("arms_rpc_result_bytes")
                    .setUnit("By")
                    .setDescription("Arms response bytes")
                    .ofLongs()
                    .build();
    requestSize =
            meter
                    .histogramBuilder("arms_rpc_request_bytes")
                    .setUnit("By")
                    .setDescription("Arms request bytes")
                    .ofLongs()
                    .build();
    delayDuration =
            meter
                    .histogramBuilder("arms_rpc_delay_seconds")
                    .setUnit("s")
                    .setDescription("The latency of rpc invocation")
                    .build();
  }

  protected void recordNormalMetrics(Context context, long duration, Predicate<Long> isSlow, Attributes allAttributes) {
    recordNormalMetrics(context, duration, isSlow, allAttributes, 1);
  }

  private void recordTotalRequestCount(long count, Attributes allAttributes, Context context) {
    // 请求数记录
    totalRequestCount.add(count, allAttributes, context);
  }

  private void recordRequestDuration(long duration, Attributes allAttributes, Context context) {
    // 耗时记录
    requestDuration.add(duration, allAttributes, context);
  }

  protected void recordNormalMetrics(Context context, long duration, Predicate<Long> isSlow, Attributes allAttributes, long count) {

    // 请求数记录
    recordTotalRequestCount(count, allAttributes, context);
    // 耗时记录
    recordRequestDuration(duration, allAttributes, context);
    // 错误数记录
    boolean error = false;
    Span span = Span.fromContext(context);
    if(span instanceof ReadableSpan) {
      error = ((ReadableSpan) span).toSpanData().getStatus() == StatusData.error();
    }
    if (error) {
      errorRequestCount.add(count, allAttributes);
    }
    boolean slow = isSlow.test(duration);
    if (slow) {
      slowRequestCount.add(count, allAttributes);
    }
  }
}
