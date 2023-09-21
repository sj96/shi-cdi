package shi.container.lifecircle;


import io.vertx.core.Future;

public interface DisposableComponent {
    Future<Void> destroy();
}
