package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import krpc.rpc.core.RpcClosure;
import krpc.rpc.core.ServiceMetas;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

public class KRPCClientInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("krpc.rpc.impl.RpcCallableBase");
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                named("sendCall")
                        .and(takesArgument(0, named("krpc.rpc.core.RpcClosure")))
                        .and(takesArgument(1, boolean.class)),
                KRPCClientInstrumentation.class.getName() + "$SendCallAdvice");

        transformer.applyAdviceToMethod(
                named("endCall")
                        .and(takesArgument(0, named("krpc.rpc.core.RpcClosure")))
                        .and(takesArgument(1, Object.class)),
                KRPCClientInstrumentation.class.getName() + "$EndCallAdvice");
    }

    public static class SendCallAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void startSpan(
                @Advice.Argument(0) RpcClosure rpcClosure,
                @Advice.FieldValue("serviceMetas") ServiceMetas serviceMetas,
                @Advice.Local("otelRequest") KRPCRequest request,
                @Advice.Local("otelContext") Context context,
                @Advice.Local("otelScope") Scope scope) {
            request = new KRPCRequest(rpcClosure, serviceMetas);
            Context parentContext = currentContext();
            if (!KRPCSingleton.clientInstrumenter().shouldStart(parentContext, request)) {
                return;
            }
            context = KRPCSingleton.clientInstrumenter().start(parentContext, request);
            request.setContext(context);
            scope = context.makeCurrent();
            KRPCSingleton.storeRequestInRpcClosure(rpcClosure, request);
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void stopSpan(
                @Advice.Thrown Throwable throwable,
                @Advice.Local("otelScope") Scope scope) {
            if (scope != null) {
                scope.close();
            }
        }
    }

    public static class EndCallAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void startSpan(
                @Advice.Argument(0) RpcClosure rpcClosure,
                @Advice.Local("otelRequest") KRPCRequest request,
                @Advice.Local("otelScope") Scope scope) {
            request = KRPCSingleton.getKrpcRequestFromRpcClosure(rpcClosure);
            if (request == null) {
                return;
            }
            Context parentContext = request.getContext();
            scope = parentContext.makeCurrent();
        }


        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void stopSpan(
                @Advice.Argument(1) Object response,
                @Advice.Local("otelRequest") KRPCRequest request,
                @Advice.Local("otelScope") Scope scope) {
            if (scope != null) {
                scope.close();
                KRPCResponse krpcResponse = new KRPCResponse(response);
                KRPCSingleton.clientInstrumenter().end(request.getContext(), request, krpcResponse, null);
            }
        }
    }
}
