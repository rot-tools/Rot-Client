// SPDX-License-Identifier: GPL-3.0-only
package fi.rotclient;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class AutismLicenseBoundaryTest {
    @Test void gplImplementationAndNoticesExistOnlyInPlusWhileLiteRemainsMit() throws Exception {
        var props=new Properties();try(var in=Files.newInputStream(Path.of("gradle.properties"))){props.load(in);}
        String version=props.getProperty("mod_version");
        for(boolean plus:new boolean[]{false,true})try(var jar=new ZipFile("build/libs/RotClient"+(plus?"Plus":"")+"-"+version+".jar")){
            var metadata=com.google.gson.JsonParser.parseString(new String(jar.getInputStream(jar.getEntry("fabric.mod.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(plus?"GPL-3.0-only":"MIT",metadata.get("license").getAsString());
            for(String name:ListHolder.CLASSES)assertEquals(plus,jar.getEntry("fi/rotclient/"+name+".class")!=null,name);
            assertEquals(plus,jar.getEntry("META-INF/licenses/autism-client/AUTISM-COPYING.txt")!=null);
            long importedClasses = jar.stream().filter(e -> e.getName().endsWith(".class")
                    && (e.getName().startsWith("fi/rotclient/Autism") || e.getName().startsWith("fi/rotclient/mixin/Autism"))).count();
            if (plus) assertTrue(importedClasses >= 16);
            else assertEquals(0, importedClasses, "GPL implementation leaked into Lite");
            if(plus){
                assertArrayEquals(Files.readAllBytes(Path.of("licenses/plus/AUTISM-COPYING.txt")),
                        jar.getInputStream(jar.getEntry("LICENSE_RotClientPlus")).readAllBytes());
                assertNotNull(jar.getEntry("META-INF/licenses/autism-client/AUTISM-CREDITS.md"));
                assertNotNull(jar.getEntry("META-INF/licenses/autism-client/SOURCE.json"));
                assertNotNull(jar.getEntry("LICENSE_RotClientOriginal-MIT"));
                assertTrue(new String(jar.getInputStream(jar.getEntry("LICENSE_RotClientPlus")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).contains("GNU GENERAL PUBLIC LICENSE"));
            }
        }
    }
    private static class ListHolder {static final String[] CLASSES={"AutismPortPolicy","AutismModuleSettings","AutismMovementRuntime","AutismToolRuntime","AutismInteractionRuntime","AutismFreeLookRuntime","AutismAirPlaceRuntime","AutismSignRuntime","AutismAirJumpPolicy","AutismAirJumpRuntime"};}
}
