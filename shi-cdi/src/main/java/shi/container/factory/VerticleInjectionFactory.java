package shi.container.factory;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.impl.verticle.CompilingClassLoader;
import io.vertx.core.spi.VerticleFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import shi.container.Container;
import shi.container.bind.Bind;

import java.util.concurrent.Callable;

@RequiredArgsConstructor
public class VerticleInjectionFactory implements VerticleFactory {
    private final Container container;

    @Override
    public String prefix() {
        return "inject";
    }

    @Override
    public void createVerticle(String verticleName, ClassLoader classLoader, Promise<Callable<Verticle>> promise) {
        var clazz = getVerticleClass(verticleName, classLoader);
        container.registry(Bind.bind(clazz).to(clazz).singleton(false));
        promise.complete(() -> new ProxyVerticle<>(container, clazz));
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private Class<Verticle> getVerticleClass(String verticleName, ClassLoader classLoader) {
        verticleName = VerticleFactory.removePrefix(verticleName);
        Class<Verticle> clazz;
        if (verticleName.endsWith(".java")) {
            var compilingLoader = new CompilingClassLoader(classLoader, verticleName);
            var className = compilingLoader.resolveMainClassName();
            clazz = (Class<Verticle>) compilingLoader.loadClass(className);
        } else {
            clazz = (Class<Verticle>) classLoader.loadClass(verticleName);
        }
        return clazz;
    }

    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }

    @RequiredArgsConstructor
    static class ProxyVerticle<T extends Verticle> implements Verticle {
        private final Container container;
        private final Class<T> domain;
        private Vertx vertx;
        private Context context;
        private T verticle;

        @Override
        public Vertx getVertx() {
            return vertx;
        }

        @Override
        public void init(Vertx vertx, Context context) {
            this.vertx = vertx;
            this.context = context;
        }

        @Override
        public void start(Promise<Void> startPromise) {
            container.getInstance(domain).onComplete(ar -> {
                if (ar.succeeded()) {
                    this.verticle = ar.result();
                    verticle.init(vertx, context);
                    try {
                        verticle.start(startPromise);
                    } catch (Exception e) {
                        startPromise.tryFail(e);
                    }
                } else {
                    startPromise.tryFail(ar.cause());
                }
            });
        }

        @Override
        public void stop(Promise<Void> stopPromise) throws Exception {
            if (verticle != null) {
                verticle.stop(stopPromise);
            } else {
                stopPromise.tryComplete();
            }
        }
    }
}
