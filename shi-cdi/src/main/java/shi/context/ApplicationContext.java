package shi.context;

import io.vertx.core.Future;
import shi.context.injectors.InjectorAdapter;
import shi.context.internal.ApplicationContextImpl;
import shi.context.binding.Bind;

import java.util.List;

public interface ApplicationContext {
    static ApplicationContext create() {
        return new ApplicationContextImpl();
    }

    ApplicationContext addInjector(InjectorAdapter injector);

    <T> Future<T> createComponent(Class<T> type);

    <T> Future<T> getComponent(Class<T> type);

    <T> Future<T> getComponent(Class<T> type, String qualifier);

    <T> Future<List<T>> getComponents(Class<T> type);

    <T> Future<T> inject(T instance);

    <T> void registry(Bind<T> bind);

    <T> void registry(Bind<T> bind, T instance);

    ApplicationContext release();
}
