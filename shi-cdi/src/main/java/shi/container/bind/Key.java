package shi.container.bind;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Key {
    private final Class<?> type;
    private final String name;
}
