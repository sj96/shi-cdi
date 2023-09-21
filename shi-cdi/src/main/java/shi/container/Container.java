package shi.container;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import shi.container.factory.ComponentFactory;
import shi.container.injectors.InjectorAdapter;
import shi.container.internal.ContainerImpl;
import shi.container.binding.Bind;

import java.util.List;

public interface Container {
    static Container create() {
        return new ContainerImpl(Vertx.vertx());
    }

    static Container create(Vertx vertx) {
        return new ContainerImpl(vertx);
    }

    Vertx vertx();

    Container addInjector(InjectorAdapter injector);

    <T> Future<T> createComponent(Class<T> type);

    <T> Future<T> getComponent(Class<T> type);

    <T> Future<T> getComponent(Class<T> type, String qualifier);

    <T> Future<List<T>> getComponents(Class<T> type);

    <T> Future<T> inject(T instance);

    <T> void registry(Bind<T> bind);

    <T> void registry(Bind<T> bind, T instance);

    <T> void registry(Bind<T> bind, ComponentFactory<T> instance);

    Future<Void> close();
}
