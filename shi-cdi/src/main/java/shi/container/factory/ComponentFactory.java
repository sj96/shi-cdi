package shi.container.factory;

import io.vertx.core.Future;

public interface ComponentFactory<T> {
    Future<T> create();
}
