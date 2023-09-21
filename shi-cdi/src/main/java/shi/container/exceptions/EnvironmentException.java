package shi.container.exceptions;

import lombok.Getter;
import shi.container.exceptions.errors.ErrorType;

@Getter
public class EnvironmentException extends RuntimeException {

    private static final String FORMATTED_MESSAGE = "EX%s - %s";

    private final ErrorType error;

    public EnvironmentException(ErrorType error) {
        super(String.format(FORMATTED_MESSAGE, error.code(), error.message()), error.throwable());
        this.error = error;
    }
}
