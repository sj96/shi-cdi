package shi.container;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import shi.container.factory.InstanceFactory;
import shi.container.injectors.FieldInjector;
import shi.container.internal.ContainerImpl;
import shi.container.bind.Bind;

import java.util.List;

public interface Container {
    static Container create() {
        return new ContainerImpl(Vertx.vertx());
    }

    static Container create(Vertx vertx) {
        return new ContainerImpl(vertx);
    }

    Vertx vertx();

    Container addInjector(FieldInjector injector);

    <T> Future<T> createInstance(Class<T> type);

    <T> Future<T> getInstance(Class<T> type);

    <T> Future<T> getInstance(Class<T> type, String qualifier);

    <T> Future<List<T>> getInstances(Class<T> type);

    <T> Future<T> inject(T instance);

    <T> void registry(Bind<T> bind);

    <T> void registry(Bind<T> bind, T instance);

    <T> void registry(Bind<T> bind, InstanceFactory<T> instance);

    Future<Void> close();
}
