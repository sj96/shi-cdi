package shi.vertx.container.resolvers;

import shi.vertx.container.annotations.ImplementedBy;

public class ImplementedByResolver implements ImplementationResolver {

    @Override
    public Class<?> supply(Class<?> type) {
        // Ignore non interfaces without component module
        if (!type.isInterface() || !type.isAnnotationPresent(ImplementedBy.class)) {
            return null;
        }
        return type.getAnnotation(ImplementedBy.class).value();
    }
}
