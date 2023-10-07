package shi.container.factory;

import io.vertx.core.Future;

public interface InstanceFactory<T> {
    Future<T> create();
}
