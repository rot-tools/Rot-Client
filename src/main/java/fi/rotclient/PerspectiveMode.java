package fi.rotclient;

/**
 * Minecraft-free mirror of vanilla client camera perspectives for policy
 * and unit tests. Maps 1:1 to {@code net.minecraft.client.CameraType}.
 */
public enum PerspectiveMode {
    FIRST_PERSON,
    THIRD_PERSON_BACK,
    THIRD_PERSON_FRONT;

    public boolean isThirdPerson() {
        return this != FIRST_PERSON;
    }
}
