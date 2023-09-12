package shi.vertx.container.context;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import shi.vertx.container.binding.Bind;
import shi.vertx.container.binding.Key;
import shi.vertx.container.exceptions.EnvironmentException;
import shi.vertx.container.errors.Errors;
import shi.vertx.container.resolvers.ImplementationResolver;

import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
class InternalBinder {

    private final List<ImplementationResolver> implementationResolvers;

    public <T> Bind<T> createBind(Key key) {
        var bind = Bind.bind(key.getType()).name(key.getName());
        lookupBind(bind);
        return (Bind<T>) bind;
    }

    public <T> Bind<T> createBind(Bind<T> bind) {
        lookupBind(bind);
        return bind;
    }

    public <T> void lookupBind(Bind<T> bind) {
        if (bind.to() == null) {
            var finder = new InternalImplementationFinder(implementationResolvers);
            bind.to(finder.find(bind.from(), bind.name()));
        }
        if (bind.to() == null) {
            if (bind.from().isInterface()) {
                throw new EnvironmentException(Errors.NO_CMPT_REGISTERED.arguments(bind.from().getName()));
            }
            bind.to(bind.from());
        }
        if (StringUtils.isEmpty(bind.name())) {
            qualify(bind);
        }
        bind.singleton(isSingleton(bind));
    }

    private boolean isSingleton(Bind<?> bind) {
        return bind.singleton() || isSingleton(bind.to()) || isSingleton(bind.from());
    }

    private boolean isSingleton(Class<?> clazz) {
        return clazz == null
                || clazz.isAnnotationPresent(Singleton.class)
                || Arrays.stream(clazz.getAnnotations())
                .anyMatch(a -> a.annotationType().isAnnotationPresent(Singleton.class));
    }

    private void qualify(final Bind<?> bind) {
        if (bind.to() == null) {
            return;
        }
        if (bind.to().isAnnotationPresent(Named.class)) {
            bind.name(bind.to().getAnnotation(Named.class).value());
            return;
        }
        Arrays.stream(bind.to().getAnnotations()).forEach(a -> {
            // With qualifier qualify with module name
            if (a.annotationType().isAnnotationPresent(Qualifier.class)) {
                bind.name(a.annotationType().getSimpleName());
            }
        });
    }

}
