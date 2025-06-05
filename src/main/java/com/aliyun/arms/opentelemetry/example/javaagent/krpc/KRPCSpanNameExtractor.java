package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;

public class KRPCSpanNameExtractor implements SpanNameExtractor<KRPCRequest> {
    @Override
    public String extract(KRPCRequest KRPCRequest) {
        return KRPCRequest.getName();
    }
}