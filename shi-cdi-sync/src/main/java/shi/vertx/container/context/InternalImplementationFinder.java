package shi.vertx.container.context;

import lombok.RequiredArgsConstructor;
import shi.vertx.container.errors.Errors;
import shi.vertx.container.exceptions.EnvironmentException;
import shi.vertx.container.resolvers.ImplementationResolver;
import shi.vertx.container.utils.ReflectionUtils;

import java.util.List;

@RequiredArgsConstructor
class InternalImplementationFinder {

    private final List<ImplementationResolver> implementationResolvers;

    public Class<?> find(Class<?> clazz, String name) {
        Class<?> candidate = null;
        for (var ip : implementationResolvers) {
            candidate = ip.supply(clazz);
            if (candidate != null) break;
        }

        if (candidate != null && !ReflectionUtils.getQualifier(candidate).equals(name)) {
            throw new EnvironmentException(Errors.NO_QUALIFIER_REGISTERED);
        }
        return candidate;
    }
}
