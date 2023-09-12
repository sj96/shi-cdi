package shi.vertx.container;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import shi.vertx.container.binding.Bind;
import shi.vertx.container.constants.Settings;
import shi.vertx.container.errors.Errors;
import shi.vertx.container.exceptions.EnvironmentException;
import shi.vertx.container.injectors.InjectorAdapter;

import java.util.Objects;
import java.util.ServiceLoader;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Environment {

    private static ApplicationContext context;

    public static ApplicationContext create(ApplicationContext contextGraph) {
        context = contextGraph;
        return context;
    }

    public static ApplicationContext create() {
        var ctx = create(ApplicationContext.create());
        var ignoreInjectors = System.getProperty(Settings.IGNORE_AUTO_INJECTORS, Boolean.FALSE.toString());
        if (!Boolean.TRUE.toString().equals(ignoreInjectors)) {
            ServiceLoader.load(InjectorAdapter.class).forEach(injector -> {
                try {
                    ctx.addInjector(injector);
                } catch (Exception e) {
                    // ignore invalid injector
                }
            });
        }
        return ctx;
    }

    public static <T> T my(Class<T> clazz) {
        return my(clazz, StringUtils.EMPTY);
    }

    public static <T> T my(Class<T> clazz, String qualifier) {
        return getContext().getInstance(clazz, qualifier);
    }

    public static <T> T provide(Class<T> clazz) {
        return provide(clazz, StringUtils.EMPTY);
    }

    public static <T> T provide(Class<T> clazz, String qualifier) {
        return getContext().provide(clazz, qualifier);
    }

    public static void registry(@NonNull Bind bind) {
        getContext().registry(bind);
    }

    public static void registry(@NonNull Bind bind, @NonNull Object instance) {
        getContext().registry(bind, instance);
    }

    public static void inject(Object instance) {
        getContext().inject(instance);
    }

    public static ApplicationContext getContext() {
        if (context == null) {
            throw new EnvironmentException(Errors.CONTEXT_NOT_CREATED);
        }
        synchronized (context) {
            return context;
        }
    }

    public static boolean isLoaded() {
        return !Objects.isNull(context);
    }

    public static void release() {
        getContext().release();
    }

    public static void destroy() {
        if (context != null) {
            synchronized (context) {
                context = null;
            }
        }
    }
}
