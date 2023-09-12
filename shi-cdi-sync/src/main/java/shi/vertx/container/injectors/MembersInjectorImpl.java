package shi.vertx.container.injectors;

import lombok.RequiredArgsConstructor;
import shi.vertx.container.Environment;
import shi.vertx.container.utils.AnnotationUtils;
import shi.vertx.container.utils.ReflectionUtils;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

@RequiredArgsConstructor
public final class MembersInjectorImpl implements MembersInjector {

    private final Map<Class<? extends InjectorAdapter>, InjectorAdapter> INJECTORS;

    @Override
    public void inject(Object instance) {
        Class<?> clazz = instance.getClass();
        inject(clazz, instance);
    }

    private void inject(Class<?> type, Object instance) {
        if (type == null) {
            return;
        }
        // Recursive call to resolve superclass
        inject(type.getSuperclass(), instance);

        for (var field : type.getDeclaredFields()) {
            //are annotated with @Inject.
            //are not final.
            //may have any otherwise valid name.
            if (!field.isAnnotationPresent(Inject.class) || Modifier.isFinal(field.getModifiers())) {
                INJECTORS.values().forEach(injector -> {
                    if (injector.isInjectable(field)) {
                        injector.inject(instance, field);
                    }
                });
                continue;
            }
            var fieldType = field.getType();
            var name = ReflectionUtils.getQualifier(field);
            ReflectionUtils.setField(field, instance, Environment.my(fieldType, name));
        }

        var methods = ReflectionUtils.findDeclaredMethods(type,
                method -> AnnotationUtils.hasAnnotation(method, Inject.class),
                method -> !Modifier.isAbstract(method.getModifiers())
        );
        for (var method : methods) {
            var parameters = resolveParameters(method);
            ReflectionUtils.invoke(method, instance, parameters);
        }
    }

    private static Object[] resolveParameters(Method method) {
        var parameters = new Object[method.getParameterCount()];
        for (var index = 0; index < parameters.length; index++) {
            var param = method.getParameters()[index];
            var paramType = param.getType();
            var paramQualifier = ReflectionUtils.getQualifier(param);
            parameters[index] = Environment.my(paramType, paramQualifier);
        }
        return parameters;
    }
}
