package shi.vertx.container.context;

import lombok.NonNull;
import lombok.Synchronized;
import org.apache.commons.lang3.StringUtils;
import shi.vertx.container.ApplicationContext;
import shi.vertx.container.binding.Bind;
import shi.vertx.container.binding.Key;
import shi.vertx.container.errors.Errors;
import shi.vertx.container.exceptions.EnvironmentException;
import shi.vertx.container.factories.InstanceFactory;
import shi.vertx.container.injectors.InjectorAdapter;
import shi.vertx.container.injectors.MembersInjectorImpl;
import shi.vertx.container.resolvers.ComponentResolver;
import shi.vertx.container.resolvers.ImplementationResolver;
import shi.vertx.container.resolvers.ImplementedByResolver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ApplicationContextImpl implements ApplicationContext {

    private static final int RIC = 10;
    private static final float RLF = 0.85f;

    private final Map<Class<? extends InjectorAdapter>, InjectorAdapter> injectors = new HashMap<>();
    private final Map<Key, Bind<?>> binders = new ConcurrentHashMap<>(RIC, RLF);
    private final Map<Key, Object> holders = new ConcurrentHashMap<>(RIC, RLF);
    private final List<ImplementationResolver> implementationResolvers = new ArrayList<>();
    private final InternalBinder internalBinder;

    public ApplicationContextImpl() {
        implementationResolvers.add(new ComponentResolver());
        implementationResolvers.add(new ImplementedByResolver());
        ServiceLoader.load(ImplementationResolver.class).forEach(implementationResolvers::add);
        internalBinder = new InternalBinder(implementationResolvers);
    }

    @Override
    public <T> T getInstance(Class<T> clazz) {
        return getInstance(clazz, StringUtils.EMPTY);
    }

    @Override
    public <T> T getInstance(Class<T> clazz, String qualifier) {
        if (StringUtils.isEmpty(qualifier)) {
            var binds = retrieveBinds(clazz);
            if (binds.isEmpty()) {
                return null;
            }
            if (binds.size() == 1) {
                return reuseOrInstantiate(binds.get(0));
            }
            for (var bind : binds) {
                if (bind.primary()) {
                    return reuseOrInstantiate(bind);
                }
            }
            throw new EnvironmentException(Errors.TOO_MANY_INSTANCES.arguments(clazz));
        }
        var bind = retrieveBind(clazz, qualifier);
        return reuseOrInstantiate(bind);
    }

    private <T> T reuseOrInstantiate(Bind<T> bind) {
        var key = bind.bindKey();
        if (!holders.containsKey(key)) {
            T instance = instantiate(bind);
            if (bind.singleton()) {
                registry(bind, instance);
            }
            return instance;
        }
        //noinspection unchecked
        return (T) holders.get(key);
    }

    @Override
    public <T> List<T> getInstances(Class<T> clazz) {
        var binds = retrieveBinds(clazz);
        var instances = new ArrayList<T>(binds.size());
        for (var bind : binds) {
            var key = bind.bindKey();
            if (!holders.containsKey(key)) {
                T instance = instantiate(bind);
                if (bind.singleton()) {
                    registry(bind, instance);
                }
                instances.add(instance);
            }
        }
        return instances;
    }

    @Override
    public <T> T provide(Class<T> clazz) {
        return provide(clazz, StringUtils.EMPTY);
    }

    @Override
    public <T> T provide(Class<T> clazz, String qualifier) {
        var bind = retrieveBind(clazz, qualifier);
        return instantiate(bind);
    }

    @Synchronized
    @Override
    public <T> ApplicationContext registry(@NonNull Bind<T> bind) {
        bind = internalBinder.createBind(bind);
        if (bind.primary() && retrieveBinds((Class<?>) bind.from()).stream().anyMatch(Bind::primary)) {
            throw new EnvironmentException(Errors.TOO_MANY_PRIMARY_IMPLEMENTATIONS.arguments(bind.from()));
        }
        binders.put(bind.bindKey(), bind);
        return this;
    }

    @Synchronized
    @Override
    public <T> ApplicationContext registry(@NonNull Bind<T> bind, @NonNull Object instance) {
        // Registry ClassImpl on InternalBinder
        //noinspection unchecked
        bind.to(instance.getClass());
        registry(bind);
        // Registry Holder
        holders.put(bind.bindKey(), instance);
        return this;
    }


    @Override
    public ApplicationContext addImplementationProvider(ImplementationResolver provider) {
        implementationResolvers.add(provider);
        return this;
    }

    @Override
    public ApplicationContext addInjector(InjectorAdapter injector) {
        injectors.put(injector.getClass(), injector);
        return this;
    }

    @Override
    public void inject(Object instance) {
        var membersInject = new MembersInjectorImpl(injectors);
        membersInject.inject(instance);
    }

    @Override
    public ApplicationContext release() {
        binders.clear();
        holders.clear();
        return this;
    }

    private <T> T instantiate(Bind<T> bind) {
        var provider = new InstanceFactory<>(this, bind);
        return provider.get();
    }

    private <T> List<Bind<T>> retrieveBinds(Class<T> clazz) {
        return binders.entrySet()
                .stream()
                .filter(entry -> clazz.isAssignableFrom(entry.getKey().getType()))
                .map(entry -> (Bind<T>) entry.getValue())
                .collect(Collectors.toList());
    }

    private <T> Bind<T> retrieveBind(Class<T> clazz, String name) {
        if (StringUtils.isEmpty(name)) {
            var binds = retrieveBinds(clazz);
            if (!binds.isEmpty() && binds.stream().noneMatch(Bind::primary)) {
                throw new EnvironmentException(Errors.TOO_MANY_INSTANCES.arguments(clazz));
            }
        }
        var key = new Key(clazz, name);
        // Find Bind on Container
        var bind = binders.computeIfAbsent(key, k -> {
            // Create Bind with magic of reflection
            return internalBinder.createBind(key);
        });
        return (Bind<T>) bind;
    }
}
