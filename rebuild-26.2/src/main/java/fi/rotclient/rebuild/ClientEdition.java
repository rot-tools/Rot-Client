package fi.rotclient.rebuild;

import java.util.List;

public interface ClientEdition {
    default void initialize() {}

    default List<ModuleCard> modules() {
        return List.of();
    }
}
