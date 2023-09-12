package shi.vertx.container;

import shi.vertx.container.binding.Bind;
import shi.vertx.container.context.ApplicationContextImpl;
import shi.vertx.container.injectors.InjectorAdapter;
import shi.vertx.container.resolvers.ImplementationResolver;

import java.util.List;

public interface ApplicationContext {

    static ApplicationContext create() {
        return new ApplicationContextImpl();
    }

    ApplicationContext addImplementationProvider(ImplementationResolver provider);

    ApplicationContext addInjector(InjectorAdapter injector);

    void inject(Object instance);

    <T> T getInstance(Class<T> clazz);

    <T> T getInstance(Class<T> clazz, String named);

    <T> List<T> getInstances(Class<T> clazz);

    <T> T provide(Class<T> clazz);

    <T> T provide(Class<T> clazz, String named);

    <T>  ApplicationContext registry(Bind<T> bind);

    <T>  ApplicationContext registry(Bind<T> bind, Object instance);

    ApplicationContext release();
}
