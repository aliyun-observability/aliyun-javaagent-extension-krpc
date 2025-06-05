package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

import javax.annotation.Nullable;

public enum KrpcAttributesExtractor implements AttributesExtractor<KRPCRequest, KRPCResponse> {
    INSTANCE;

    @Override
    public void onStart(AttributesBuilder attributes, Context parentContext, KRPCRequest KRPCRequest) {
        attributes.put("krpc.serviceName", KRPCRequest.getServiceName());
    }

    @Override
    public void onEnd(AttributesBuilder attributes, Context context, KRPCRequest KRPCRequest, @Nullable KRPCResponse KRPCResponse, @Nullable Throwable error) {
    }
}
