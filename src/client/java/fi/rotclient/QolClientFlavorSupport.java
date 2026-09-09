package fi.rotclient;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Resolves optional Plus client hooks. Legit classpaths have no provider.
 */
public final class QolClientFlavorSupport {
    private static final QolClientFlavorHooks HOOKS = load();

    private QolClientFlavorSupport() {
    }

    private static QolClientFlavorHooks load() {
        ServiceLoader<QolClientFlavorHooks> loader =
                ServiceLoader.load(QolClientFlavorHooks.class);
        Iterator<QolClientFlavorHooks> iterator = loader.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return QolClientFlavorHooks.NONE;
    }

    public static QolClientFlavorHooks hooks() {
        return HOOKS;
    }
}
