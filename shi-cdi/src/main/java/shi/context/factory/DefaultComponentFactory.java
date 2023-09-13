package shi.context.factory;

import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import shi.context.annotation.Inject;
import shi.context.binding.Bind;
import shi.context.exceptions.EnvironmentException;
import shi.context.exceptions.errors.Errors;
import shi.context.internal.ApplicationContextImpl;
import shi.context.utils.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
@RequiredArgsConstructor
public class DefaultComponentFactory<T> implements ComponentFactory<T> {
    private final ApplicationContextImpl context;
    private final Bind<T> bind;

    @Override
    public Future<T> create() {
        return context.vertx().executeBlocking(() -> {
                    var constructors = allowedConstructors(bind.to());
                    if (constructors.isEmpty()) {
                        throw new EnvironmentException(Errors.FAILED_INSTANTIATION.arguments(bind.to().getName()));
                    }
                    if (constructors.size() > 1 && log.isLoggable(Level.WARNING)) {
                        log.warning(String.format("More than one valid constructor founded for class %s", bind.to()));
                    }
                    return constructors.get(0);
                }, false)
                .compose(this::instantiate)
                .compose(context::inject)
                .map(instance -> {
                    postConstruct(instance.getClass(), instance);
                    return instance;
                })
                .map(bind.from()::cast);
    }


    private Future<T> instantiate(Constructor<?> candidate) {
        var injectAllParameters = candidate.isAnnotationPresent(Inject.class) || candidate.getDeclaringClass().getConstructors().length == 1;
        return resolveParameters(candidate, injectAllParameters)
                .recover(t -> {
                    if (t instanceof EnvironmentException && ((EnvironmentException) t).getError().code() == Errors.CIRCULAR_INJECTION.code())
                        throw new EnvironmentException(Errors.FAILED_INSTANTIATION
                                .arguments(candidate.getDeclaringClass())
                                .throwable(t)
                        );
                    return Future.failedFuture(t);
                })
                .map(parameters -> {
                    try {
                        return ReflectionUtils.newInstance(candidate, parameters);
                    } catch (Exception e) {
                        throw new EnvironmentException(Errors.FAILED_INSTANTIATION
                                .arguments(candidate.getDeclaringClass())
                                .throwable(e)
                        );
                    }
                });
    }

    private Future<Object[]> resolveParameters(Constructor<?> candidate, boolean injectAllParameters) {
        return context.resolveParameters(candidate.getParameters(), injectAllParameters);
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
