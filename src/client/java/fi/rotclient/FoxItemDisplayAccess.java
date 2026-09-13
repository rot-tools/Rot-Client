package fi.rotclient;

/**
 * Render-state extension for local Fox replacement of item-display wall art.
 */
public interface FoxItemDisplayAccess {
    boolean rotclient$foxReplace();

    void rotclient$setFoxReplace(boolean replace);

    float rotclient$foxU0();

    float rotclient$foxV0();

    float rotclient$foxU1();

    float rotclient$foxV1();

    void rotclient$setFoxUv(float u0, float v0, float u1, float v1);
}
