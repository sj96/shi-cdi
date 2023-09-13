package shi.context.injectors;

import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import shi.context.annotation.Inject;
import shi.context.internal.ApplicationContextImpl;
import shi.context.utils.AnnotationUtils;
import shi.context.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

@RequiredArgsConstructor
public final class MembersInjectorImpl implements MembersInjector {

    private final ApplicationContextImpl context;
    private final Map<Class<? extends InjectorAdapter>, InjectorAdapter> injectors;

    @Override
    public Future<Void> inject(Object instance) {
        var clazz = instance.getClass();
        return inject(clazz, instance);
    }

    private Future<Void> inject(Class<?> type, Object instance) {
        if (type == null || Object.class.equals(type)) {
            return Future.succeededFuture();
        }
        // Recursive call to resolve superclass
        return inject(type.getSuperclass(), instance)
                .compose(v -> injectFields(type, instance))
                .compose(v -> injectMethods(type, instance));
    }

    private Future<Void> injectMethods(Class<?> type, Object instance) {
        var future = Future.<Void>succeededFuture();
        var methods = ReflectionUtils.findDeclaredMethods(type,
                method -> AnnotationUtils.hasAnnotation(method, Inject.class),
                method -> !Modifier.isAbstract(method.getModifiers())
        );
        for (var method : methods) {
            future = future.compose(v2 -> context
                    .resolveParameters(method.getParameters(), true)
                    .map(parameters -> {
                        ReflectionUtils.invoke(method, instance, parameters);
                        return null;
                    })
            );
        }
        return future;
    }

    private Future<Void> injectFields(Class<?> type, Object instance) {
        var future = Future.<Void>succeededFuture();
        for (var field : type.getDeclaredFields()) {
            future = future.compose(v2 -> {
                //are annotated with @Inject.
                //are not final.
                //may have any otherwise valid name.
                if (!field.isAnnotationPresent(Inject.class) || Modifier.isFinal(field.getModifiers())) {
                    return tryInjectWithAdapter(instance, field);
                }
                var fieldType = field.getType();
                var name = ReflectionUtils.getQualifier(field);
                return context.getComponent(fieldType, name).map(component -> {
                    ReflectionUtils.setField(field, instance, component);
                    return null;
                });
            });
        }
        return future;
    }

    private Future<Void> tryInjectWithAdapter(Object instance, Field field) {
        var future = Future.<Void>succeededFuture();
        for (var injector : injectors.values()) {
            if (injector.isInjectable(field)) {
                future = future.compose(v -> injector.inject(instance, field));
            }
        }
        return future;
    }
}
