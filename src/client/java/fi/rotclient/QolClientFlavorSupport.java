package fi.rotclient;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Resolves optional Plus client hooks. Legit classpaths have no provider.
 */
public final class QolClientFlavorSupport {
    private static volatile QolClientFlavorHooks HOOKS = load();

    private QolClientFlavorSupport() {
    }

    private static QolClientFlavorHooks load() {
        ClassLoader loader = QolClientFlavorHooks.class.getClassLoader();
        ServiceLoader<QolClientFlavorHooks> services = loader == null
                ? ServiceLoader.load(QolClientFlavorHooks.class)
                : ServiceLoader.load(QolClientFlavorHooks.class, loader);
        Iterator<QolClientFlavorHooks> iterator = services.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        iterator = ServiceLoader.load(QolClientFlavorHooks.class).iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return QolClientFlavorHooks.NONE;
    }

    public static void install(QolClientFlavorHooks hooks) {
        if (hooks != null) {
            HOOKS = hooks;
        }
    }

    public static QolClientFlavorHooks hooks() {
        return HOOKS;
    }
}
