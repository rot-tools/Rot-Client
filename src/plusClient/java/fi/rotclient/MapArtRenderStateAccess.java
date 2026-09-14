package fi.rotclient;

/**
 * Small render-state extension used only by the local map-art renderer.
 * Values are UV bounds within the selected local image.
 */
public interface MapArtRenderStateAccess {
    float rotclient$mapArtU0();

    float rotclient$mapArtV0();

    float rotclient$mapArtU1();

    float rotclient$mapArtV1();

    void rotclient$setMapArtUv(float u0, float v0, float u1, float v1);
}
