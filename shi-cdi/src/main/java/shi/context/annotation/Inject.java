package shi.context.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.CONSTRUCTOR,
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.PARAMETER
})
public @interface Inject {
}
