package shi.vertx.container.factories;

import lombok.extern.java.Log;
import shi.vertx.container.ApplicationContext;
import shi.vertx.container.ParameterizedTypeRetention;
import shi.vertx.container.binding.Bind;
import shi.vertx.container.errors.Errors;
import shi.vertx.container.exceptions.EnvironmentException;
import shi.vertx.container.utils.ReflectionUtils;

import javax.inject.Inject;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;


@Log
public class InstanceFactory<T> implements Factory<T> {
    private final ApplicationContext context;
    private final Bind<T> bind;

    public InstanceFactory(ApplicationContext context, Bind<T> bind) {
        this.context = context;
        this.bind = bind;
    }

    public T get() {
        var constructors = allowedConstructors(bind.to());
        if (constructors.isEmpty()) {
            throw new EnvironmentException(Errors.FAILED_INSTANTIATION.arguments(bind.to().getName()));
        }
        if (constructors.size() > 1 && log.isLoggable(Level.WARNING)) {
            log.warning(String.format("More than one valid constructor founded for class %s", bind.to()));
        }
        var candidate = constructors.get(0);
        T instance = instantiate(candidate);
        context.inject(instance);
        postConstruct(instance.getClass(), instance);
        return instance;
    }

    private T instantiate(Constructor<?> candidate) {
        try {
            var injectAllParameters = candidate.isAnnotationPresent(Inject.class);
            var parameters = resolveParameters(candidate, injectAllParameters);
            ReflectionUtils.makeAccessible(candidate);
            var instance = (T) candidate.newInstance(parameters);
            var type = candidate.getDeclaringClass();
            if (ReflectionUtils.collectInterfaces(type).contains(ParameterizedTypeRetention.class)) {
                var types = ((ParameterizedType) type.getGenericSuperclass()).getActualTypeArguments();
                ((ParameterizedTypeRetention) instance).setTypes(types);
            }
            return instance;
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new EnvironmentException(Errors.FAILED_INSTANTIATION.arguments(candidate.getDeclaringClass()).throwable(e));
        }
    }

    private Object[] resolveParameters(Constructor<?> candidate, boolean injectAllParameters) {
        var parameters = new Object[candidate.getParameterCount()];
        for (var index = 0; index < parameters.length; index++) {
            var param = candidate.getParameters()[index];
            Object paramValue = null;
            if (injectAllParameters || param.isAnnotationPresent(Inject.class)) {
                paramValue = resolveParam(param);
            }
            parameters[index] = paramValue;
        }
        return parameters;
    }

    private Object resolveParam(Parameter param) {
        Object paramValue;
        var type = param.getType();
        var name = ReflectionUtils.getQualifier(param);
        if (Collection.class.isAssignableFrom(type)) {
            var parameterizedType = (ParameterizedType) param.getParameterizedType();
            var elementType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            paramValue = context.getInstances(elementType);
            if (Set.class.isAssignableFrom(type)) {
                paramValue = new HashSet<>((Collection<?>) paramValue);
            }
        } else {
            paramValue = context.getInstance(type, name);
        }
        return paramValue;
    }

    private void postConstruct(Class<?> type, Object instance) {
        // Recursive call to resolve superclass
        if (type.getSuperclass() != null) {
            postConstruct(type.getSuperclass(), instance);
        }
        // JSR-250  PostConstruct
        for (var method : ReflectionUtils.getPostConstructMethods(type)) {
            ReflectionUtils.invoke(method, instance);
        }
    }

    private List<Constructor<?>> allowedConstructors(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .sorted(Comparator.comparing(c -> c.isAnnotationPresent(Inject.class) ? 0 : 1))
                .collect(Collectors.toList());
    }
}
