package shi.container.exceptions.errors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@AllArgsConstructor
@RequiredArgsConstructor
@Accessors(fluent = true)
public class ErrorType implements Serializable {

    @Getter
    private final int code;
    private final String message;
    @Getter
    @Setter
    private Throwable throwable;
    private transient Object[] arguments;

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
