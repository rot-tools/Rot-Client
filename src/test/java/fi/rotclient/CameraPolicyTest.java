package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class CameraPolicyTest {
    @Test
    void cameraOffAllowsVanillaPerspective() {
        assertEquals(
                PerspectiveMode.FIRST_PERSON,
                CameraPolicy.resolve(false, PerspectiveMode.FIRST_PERSON));
        assertEquals(
                PerspectiveMode.THIRD_PERSON_BACK,
                CameraPolicy.resolve(false, PerspectiveMode.THIRD_PERSON_BACK));
        assertEquals(
                PerspectiveMode.THIRD_PERSON_FRONT,
                CameraPolicy.resolve(false, PerspectiveMode.THIRD_PERSON_FRONT));
        assertTrue(CameraPolicy.allows(false, PerspectiveMode.THIRD_PERSON_BACK));
        assertTrue(CameraPolicy.allows(false, PerspectiveMode.THIRD_PERSON_FRONT));
        assertEquals(
                PerspectiveMode.THIRD_PERSON_BACK,
                CameraPolicy.cycle(false, PerspectiveMode.FIRST_PERSON));
        assertEquals(
                PerspectiveMode.THIRD_PERSON_FRONT,
                CameraPolicy.cycle(false, PerspectiveMode.THIRD_PERSON_BACK));
        assertEquals(
                PerspectiveMode.FIRST_PERSON,
                CameraPolicy.cycle(false, PerspectiveMode.THIRD_PERSON_FRONT));
    }

    @Test
    void cameraOnKeepsFirstPersonAndRearThirdPerson() {
        assertEquals(
                PerspectiveMode.FIRST_PERSON,
                CameraPolicy.resolve(true, PerspectiveMode.FIRST_PERSON));
        assertEquals(
                PerspectiveMode.THIRD_PERSON_BACK,
                CameraPolicy.resolve(true, PerspectiveMode.THIRD_PERSON_BACK));
        assertEquals(
                PerspectiveMode.FIRST_PERSON,
                CameraPolicy.resolve(true, PerspectiveMode.THIRD_PERSON_FRONT));
        assertEquals(
                PerspectiveMode.THIRD_PERSON_BACK,
                CameraPolicy.cycle(true, PerspectiveMode.FIRST_PERSON));
        assertEquals(
                PerspectiveMode.FIRST_PERSON,
                CameraPolicy.cycle(true, PerspectiveMode.THIRD_PERSON_BACK));
    }

    @Test
    void cameraOnAllowsBackThirdPerson() {
        assertTrue(CameraPolicy.allows(true, PerspectiveMode.THIRD_PERSON_BACK));
        assertEquals(
                PerspectiveMode.THIRD_PERSON_BACK,
                CameraPolicy.resolve(true, PerspectiveMode.THIRD_PERSON_BACK));
    }

    @Test
    void cameraOnRejectsFrontThirdPerson() {
        assertFalse(CameraPolicy.allows(true, PerspectiveMode.THIRD_PERSON_FRONT));
        assertEquals(
                PerspectiveMode.FIRST_PERSON,
                CameraPolicy.resolve(true, PerspectiveMode.THIRD_PERSON_FRONT));
    }

    @Test
    void enablingCameraPreservesRearViewButRemovesFrontView() {
        assertEquals(
                PerspectiveMode.THIRD_PERSON_BACK,
                CameraPolicy.resolve(true, PerspectiveMode.THIRD_PERSON_BACK));
        assertEquals(
                PerspectiveMode.FIRST_PERSON,
                CameraPolicy.resolve(true, PerspectiveMode.THIRD_PERSON_FRONT));
    }
}
