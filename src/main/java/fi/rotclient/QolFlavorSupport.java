package fi.rotclient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Resolves the optional Plus flavor. Legit builds have no provider on the
 * classpath, so {@link QolFlavorExtension#NONE} is used.
 */
public final class QolFlavorSupport {
    private static volatile QolFlavorExtension EXTENSION = load();

    private QolFlavorSupport() {
    }

    private static QolFlavorExtension load() {
        ClassLoader loader = QolFlavorExtension.class.getClassLoader();
        ServiceLoader<QolFlavorExtension> services = loader == null
                ? ServiceLoader.load(QolFlavorExtension.class)
                : ServiceLoader.load(QolFlavorExtension.class, loader);
        Iterator<QolFlavorExtension> iterator = services.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        ServiceLoader<QolFlavorExtension> context =
                ServiceLoader.load(QolFlavorExtension.class);
        iterator = context.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return QolFlavorExtension.NONE;
    }

    /**
     * Plus client entrypoint installs the real extension after the shared
     * initializer may have already cached a Lite catalog snapshot.
     */
    public static void install(QolFlavorExtension extension) {
        if (extension == null) {
            return;
        }
        EXTENSION = extension;
        QolUtilityCatalog.reloadFlavorModules();
        RotClientProfilePresets.reloadFlavorPresets();
    }

    public static QolFlavorExtension extension() {
        return EXTENSION;
    }

    public static boolean isPlus() {
        return EXTENSION.isPlus();
    }

    public static String productName() {
        return EXTENSION.productName();
    }

    public static String productHeader() {
        return EXTENSION.productHeader();
    }

    public static String modId() {
        return EXTENSION.modId();
    }

    public static List<QolUtilityCatalog.ModuleDef> extraModules() {
        List<QolUtilityCatalog.ModuleDef> extras = EXTENSION.extraModules();
        return extras == null ? List.of() : extras;
    }

    public static List<QolUtilityCatalog.ModuleDef> extraModulesNamed(String id) {
        if (id == null || id.isBlank()) {
            return List.of();
        }
        List<QolUtilityCatalog.ModuleDef> out = new ArrayList<>();
        for (QolUtilityCatalog.ModuleDef module : extraModules()) {
            if (module != null && id.equals(module.id())) {
                out.add(module);
            }
        }
        return List.copyOf(out);
    }

    public static List<QolUtilityCatalog.SettingDef> extraSettings(String moduleId) {
        List<QolUtilityCatalog.SettingDef> extras = EXTENSION.extraSettings(moduleId);
        return extras == null ? List.of() : extras;
    }
}
