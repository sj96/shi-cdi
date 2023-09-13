package shi.context.internal;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import shi.context.ApplicationContext;
import shi.context.annotation.Inject;
import shi.context.binding.Bind;
import shi.context.binding.Key;
import shi.context.exceptions.EnvironmentException;
import shi.context.exceptions.errors.Errors;
import shi.context.factory.ComponentFactory;
import shi.context.factory.DefaultComponentFactory;
import shi.context.injectors.InjectorAdapter;
import shi.context.injectors.MembersInjectorImpl;
import shi.context.utils.BinderUtils;
import shi.context.utils.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ApplicationContextImpl implements ApplicationContext {
    private static final Logger log = LoggerFactory.getLogger(ApplicationContextImpl.class);

    private static final Map<Class<? extends InjectorAdapter>, InjectorAdapter> INJECTORS;

    static {
        INJECTORS = new HashMap<>();
        for (var injector : ServiceLoader.load(InjectorAdapter.class)) {
            INJECTORS.put(injector.getClass(), injector);
        }
    }

    private final Map<Class<?>, Set<Key>> allKeysByType = new ConcurrentHashMap<>(64);
    private final Map<Key, Bind> binders = new ConcurrentHashMap<>(256);
    private final Map<Bind, Object> holders = new ConcurrentHashMap<>(16);
    private final Set<Bind> lockers = Collections.synchronizedSet(new HashSet<>());
    private final Vertx vertx;

    public ApplicationContextImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public ApplicationContext addInjector(InjectorAdapter injector) {
        INJECTORS.put(injector.getClass(), injector);
        return this;
    }

    @Override
    public <T> Future<T> createComponent(Class<T> type) {
        return doCreateComponent(Bind.bind(type).to(type));
    }

    @Override
    public <T> Future<T> getComponent(Class<T> type) {
        var bind = this.findBind(type, "");
        return doGetComponent(bind);
    }

    @Override
    public <T> Future<T> getComponent(Class<T> type, String qualifier) {
        var bind = this.findBind(type, qualifier);
        return doGetComponent(bind);
    }

    private <T> Future<T> doGetComponent(Bind<T> bind) {
        synchronized (holders) {
            if (holders.containsKey(bind)) {
                return vertx.executeBlocking(() -> (T) holders.get(bind), false)
                        .compose(holder -> {
                            if (holder instanceof ComponentFactory) {
                                log.warn(String.format("create component %s by factory", bind.from()));
                                return ((ComponentFactory<T>) holder).create();
                            }
                            log.warn(String.format("reuse component %s", bind.to()));
                            return Future.succeededFuture(holder);
                        });
            }
            if (bind.singleton()) {
                synchronized (lockers) {
                    if (lockers.contains(bind)) {
                        throw new EnvironmentException(Errors.CIRCULAR_INJECTION
                                .arguments(bind.from(), bind.name())
                        );
                    }
                    lockers.add(bind);
                }
            }
            return doCreateComponent(bind).andThen(ar -> {
                synchronized (holders) {
                    if (ar.succeeded() && bind.singleton()) {
                        holders.put(bind, ar.result());
                    }
                }
                synchronized (lockers) {
                    lockers.remove(bind);
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Bind<T> findBind(Class<T> type, String qualifier) {
        var keys = allKeysByType.get(type);
        if (keys == null || keys.isEmpty()) {
            keys = allKeysByType.entrySet().stream()
                    .filter(entry -> type.isAssignableFrom(entry.getKey()))
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toSet());
        }
        if (qualifier == null || qualifier.isBlank()) {
            var binds = keys.stream()
                    .map(binders::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (binds.isEmpty()) {
                throw new EnvironmentException(Errors.NO_QUALIFIER_REGISTERED
                        .arguments(type, qualifier));
            }
            if (binds.size() == 1) {
                return binds.get(0);
            }
            return (Bind<T>) binds.stream()
                    .filter(Bind::primary)
                    .findFirst()
                    .orElseThrow(() -> new EnvironmentException(Errors.TOO_MANY_INSTANCES
                            .arguments(type)
                    ));
        }
        return (Bind<T>) keys.stream()
                .filter(k -> qualifier.equals(k.getName()))
                .findFirst()
                .map(binders::get)
                .orElseThrow(() -> new EnvironmentException(Errors.NO_QUALIFIER_REGISTERED
                        .arguments(type, qualifier)
                ));
    }

    private <T> Future<T> doCreateComponent(Bind<T> bind) {
        log.warn(String.format("create component %s", bind.to()));
        var factory = new DefaultComponentFactory<>(this, bind);
        return factory.create();
    }

    @Override
    public <T> Future<List<T>> getComponents(Class<T> type) {
        if (type == null) return Future.succeededFuture(Collections.emptyList());
//        var binds = allKeysByType.entrySet()
//                .stream()
//                .filter(e -> type.isAssignableFrom(e.getKey()))
//                .flatMap(e -> e.getValue().stream())
//                .map(binders::get)
//                .collect(Collectors.toList());
//        var future = Future.succeededFuture(new ArrayList<T>(binds.size()));
//        for (var bind : binds) {
//            future = future.compose(list -> doGetComponent(bind)
//                    .map(holder -> {
//                        list.add((T) holder);
//                        return list;
//                    })
//            );
//        }
//        return future.map(list -> list);
        var futures = new ArrayList<Future<T>>();
        allKeysByType.entrySet()
                .stream()
                .filter(e -> type.isAssignableFrom(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .map(binders::get)
                .map(this::<T>doGetComponent)
                .forEach(futures::add);
        return Future.all(futures).map(CompositeFuture::list);
    }

    @Override
    public <T> Future<T> inject(T instance) {
        var membersInject = new MembersInjectorImpl(this, INJECTORS);
        return membersInject.inject(instance).map(instance);
    }

    @Override
    public <T> void registry(Bind<T> bind) {
        if (bind == null || bind.from() == null) {
            return;
        }
        if (bind.to() != null && (bind.to().isInterface()
                || Modifier.isAbstract(bind.to().getModifiers())
                || (!bind.from().isAssignableFrom(bind.to())) && !ComponentFactory.class.isAssignableFrom(bind.to()))) {
            throw new EnvironmentException(Errors.INVALID_BINDING
                    .arguments(bind.from(), bind.name(), bind.to(), bind.to())
            );
        }
        bind.name(bind.name());
        bind.singleton(BinderUtils.isSingleton(bind));
        bind.primary(BinderUtils.isPrimary(bind));
        var key = bind.bindKey();
        if (binders.containsKey(key)) {
            throw new EnvironmentException(Errors.DUPLICATE_REGISTERED
                    .arguments(bind.from(), bind.name())
            );
        }
        var types = allKeysByType.computeIfAbsent(key.getType(), l -> new HashSet<>());
        if (bind.primary() && types.stream()
                .map(binders::get)
                .filter(Objects::nonNull)
                .anyMatch(Bind::primary)) {
            throw new EnvironmentException(Errors.TOO_MANY_PRIMARY_IMPLEMENTATIONS
                    .arguments(bind.from(), bind.name())
            );
        }
        types.add(key);
        binders.put(key, bind);
    }

    @Override
    public <T> void registry(Bind<T> bind, T instance) {
        if (bind == null || instance == null) return;
        bind.to(instance.getClass());
        registry(bind);
        holders.put(bind, instance);
    }

    @Override
    public <T> void registry(Bind<T> bind, ComponentFactory<T> instance) {
        if (bind == null || instance == null) return;
        bind.to(instance.getClass());
        registry(bind);
        holders.put(bind, instance);
    }

    @Override
    public ApplicationContext release() {
        allKeysByType.clear();
        binders.clear();
        holders.clear();
        return this;
    }

    @Override
    public Vertx vertx() {
        return vertx;
    }

    public Future<Object[]> resolveParameters(Parameter[] parameters, boolean injectAllParameters) {
//        var size = parameters.length;
//        var future = Future.succeededFuture(new ArrayList<>(size));
//        for (var param : parameters) {
//            future = future.compose(resolveParameters -> {
//                if (injectAllParameters || param.isAnnotationPresent(Inject.class)) {
//                    return resolveParameter(param).map(resolveParameter -> {
//                        resolveParameters.add(resolveParameter);
//                        return resolveParameters;
//                    });
//                }
//                resolveParameters.add(null);
//                return Future.succeededFuture(resolveParameters);
//            });
//        }
//        return future.map(ArrayList::toArray);

        var futures = Arrays.stream(parameters)
                .map(param -> {
                    if (injectAllParameters || param.isAnnotationPresent(Inject.class)) {
                        return resolveParameter(param);
                    }
                    return Future.succeededFuture();
                })
                .collect(Collectors.toList());
        return Future.all(futures).map(composite -> composite.list().toArray());
    }

    private Future<?> resolveParameter(Parameter param) {
        var type = param.getType();
        var name = ReflectionUtils.getQualifier(param);
        if (Collection.class.isAssignableFrom(type)) {
            var parameterizedType = (ParameterizedType) param.getParameterizedType();
            var elementType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            return getComponents(elementType).map(paramValue -> {
                if (Set.class.isAssignableFrom(type)) {
                    return new HashSet<>((Collection<?>) paramValue);
                }
                return paramValue;
            });
        } else {
            return getComponent(type, name);
        }
    }
}
