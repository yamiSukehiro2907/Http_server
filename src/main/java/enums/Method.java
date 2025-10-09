package enums;

public enum Method {

    GET,
    POST,
    DELETE,
    PUT,
    PATCH,
    OPTIONS;

    public static Method fromString(String methodString) {
        if (methodString == null || methodString.trim().isEmpty()) return null;
        try {
            return Method.valueOf(methodString.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isSupported(Method method) {
        return method == GET || method == POST;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
