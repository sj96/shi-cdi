package shi.vertx.container.errors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@RequiredArgsConstructor
@Accessors(fluent = true)
public class ErrorType {

    @Getter
    private final int code;
    private final String message;
    @Getter
    @Setter
    private Throwable throwable;
    private Object[] arguments;

    public static ErrorType create(int code, String message) {

        return new ErrorType(code, message);
    }

    public ErrorType arguments(Object... arguments) {
        this.arguments = arguments;
        return this;
    }

    public String message() {

        return String.format(message, arguments);
    }
}
