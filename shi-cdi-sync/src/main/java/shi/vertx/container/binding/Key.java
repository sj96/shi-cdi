package shi.vertx.container.binding;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Key {
    private final Class<?> type;
    private final String name;
}
