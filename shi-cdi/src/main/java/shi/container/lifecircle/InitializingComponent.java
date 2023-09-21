package shi.container.lifecircle;


import io.vertx.core.Future;

public interface InitializingComponent {
    Future<Void> afterPropertiesSet();
}
