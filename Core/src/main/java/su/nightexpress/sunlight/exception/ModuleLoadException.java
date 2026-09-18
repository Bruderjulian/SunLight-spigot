package su.nightexpress.sunlight.exception;

public class ModuleLoadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ModuleLoadException(String message) {
        super(message);
    }
}
