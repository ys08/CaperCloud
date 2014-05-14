package capercloud.exception;

public class IllegalCredentialsException extends Exception {

    public IllegalCredentialsException() {
        super();
    }

    public IllegalCredentialsException(String message) {
        super(message);
    }

    public IllegalCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalCredentialsException(Throwable cause) {
        super(cause);
    }
}

