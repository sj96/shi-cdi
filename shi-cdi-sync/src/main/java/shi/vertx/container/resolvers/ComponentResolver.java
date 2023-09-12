package shi.vertx.container.resolvers;

import shi.vertx.container.exceptions.EnvironmentException;
import shi.vertx.container.annotations.Component;
import shi.vertx.container.errors.Errors;

public class ComponentResolver implements ImplementationResolver {

    private static final String SUFIX_DEFAULT_IMPL = "Impl";
    private static final String PCK_DEFAULT_IMPL = "%s.impl.%sImpl";

    @Override
    public Class<?> supply(Class<?> type) {
        // Ignore non interfaces without component module
        if (!type.isInterface()) {
            if (type.isAnnotationPresent(Component.class)) {
                return type;
            }
            return null;
        }

        // Find implementation class by conventions
        try {
            return Class.forName(type.getName().concat(SUFIX_DEFAULT_IMPL));
        } catch (ClassNotFoundException e) {
            // Next attempt
        }
        try {
            return Class.forName(String.format(PCK_DEFAULT_IMPL, type.getPackage().getName(), type.getSimpleName()));
        } catch (ClassNotFoundException e) {
            throw new EnvironmentException(Errors.NO_CMPT_REGISTERED.arguments(type.getName()).throwable(e));
        }
    }
}
