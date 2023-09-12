package shi.vertx.container.annotations;

import javax.inject.Singleton;
import java.lang.annotation.*;

@Documented
@Singleton
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImplementedBy {
    Class<?> value();
}
