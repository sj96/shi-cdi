package shi.vertx.container.errors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class Errors {

    public static final ErrorType CONTEXT_NOT_CREATED = ErrorType.create(0,
            "Application Context is not created. Make sure the environment has been properly loaded. Help: Call  org.jspare.core.Environmente.create()");

    public static final ErrorType NO_CMPT_REGISTERED = ErrorType.create(1000,
            "%s don't have default implementation class. Provide default implementation or registry one.");

    public static final ErrorType FAILED_INSTANTIATION = ErrorType.create(1001, "Failed to instantiate class. No valid constructors founded for class %s.");

    public static final ErrorType NO_QUALIFIER_REGISTERED = ErrorType.create(1004,
            "No implementation registered for class %s with Qualifier [%s]");

    public static final ErrorType TOO_MANY_INSTANCES = ErrorType.create(1005,
            "%s too many implementation classes. Provide primary implementation or registry one.");
    public static final ErrorType TOO_MANY_PRIMARY_IMPLEMENTATIONS = ErrorType.create(1006,
            "%s registry too many primary implementations. Registry only primary registry.");

    public static final ErrorType UNMAPPED = ErrorType.create(1999, "Environment Exception called by another Throwable.");
}
