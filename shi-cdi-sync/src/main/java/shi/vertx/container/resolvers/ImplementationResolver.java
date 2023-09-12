package shi.vertx.container.resolvers;

public interface ImplementationResolver {
    Class<?> supply(Class<?> type);
}
