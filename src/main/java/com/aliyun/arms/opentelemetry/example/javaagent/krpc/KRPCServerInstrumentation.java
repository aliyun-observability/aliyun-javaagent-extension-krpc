package com.aliyun.arms.opentelemetry.example.javaagent.krpc;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import krpc.rpc.core.RpcClosure;
import krpc.rpc.core.RpcContextData;
import krpc.rpc.core.ServerContextData;
import krpc.rpc.core.ServiceMetas;
import krpc.rpc.core.proto.RpcMeta;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class KRPCServerInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("krpc.rpc.impl.RpcCallableBase");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
            named("continue1")
                    .and(takesArgument(0, named("krpc.rpc.core.ServerContextData")))
                    .and(takesArgument(1, named("krpc.rpc.core.RpcData"))),
            KRPCServerInstrumentation.class.getName() + "$ReceiveAdvice");

    transformer.applyAdviceToMethod(
            named("endReq")
                    .and(takesArgument(0, named("krpc.rpc.core.RpcClosure"))),
            KRPCServerInstrumentation.class.getName() + "$EndReqAdvice");
  }

  public static class ReceiveAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void startSpan(
            @Advice.Argument(0) ServerContextData serverContextData,
            @Advice.Argument(1) krpc.rpc.core.RpcData rpcData,
            @Advice.FieldValue("serviceMetas") ServiceMetas serviceMetas,
            @Advice.Local("otelRequest") KRPCRequest request,
            @Advice.Local("otelContext") Context context,
            @Advice.Local("otelScope") Scope scope) {
      if (rpcData.getMeta().getDirection() != RpcMeta.Direction.REQUEST) {
        return;
      }
      request = new KRPCRequest(rpcData.getMeta(), serviceMetas);
      Context parentContext = currentContext();
      if (!KRPCSingleton.serverInstrumenter().shouldStart(parentContext, request)) {
        return;
      }
      context = KRPCSingleton.serverInstrumenter().start(parentContext, request);
      scope = context.makeCurrent();
      request.setContext(context);
      VirtualField.find(RpcContextData.class, KRPCRequest.class).set(serverContextData, request);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
            @Advice.Local("otelRequest") KRPCRequest request,
            @Advice.Local("otelContext") Context context,
            @Advice.Local("otelScope") Scope scope) {
      if (scope != null) {
        scope.close();
      }
    }
  }

  public static class EndReqAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void startSpan(
            @Advice.Argument(0) RpcClosure rpcClosure,
            @Advice.Local("otelRequest") KRPCRequest request,
            @Advice.Local("otelScope") Scope scope) {
      request = VirtualField.find(RpcContextData.class, KRPCRequest.class).get(rpcClosure.getCtx());
      if(request == null) {
        return;
      }
      Context parentContext = request.getContext();
      scope = parentContext.makeCurrent();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
            @Advice.Local("otelRequest") KRPCRequest request,
            @Advice.Local("otelScope") Scope scope) {
      if (scope != null) {
        scope.close();
        KRPCSingleton.serverInstrumenter()
                .end(request.getContext(), request, null, null);
      }
    }
  }
}
