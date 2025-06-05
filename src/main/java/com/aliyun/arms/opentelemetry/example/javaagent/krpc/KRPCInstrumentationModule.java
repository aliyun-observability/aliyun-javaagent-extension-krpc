package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;

import java.util.Arrays;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class KRPCInstrumentationModule extends InstrumentationModule {

    public KRPCInstrumentationModule() {
        super("my-krpc");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return Arrays.asList(new KRPCClientInstrumentation(), new KRPCServerInstrumentation());
    }

    @Override
    public boolean isHelperClass(String className) {
        return className.startsWith("com.aliyun.arms.opentelemetry.example.javaagent");
    }
}
