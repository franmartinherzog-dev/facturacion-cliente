package com.facturacion.cliente;

public final class Session {

    private static String token;
    private static String username;

    private Session() {
    }

    public static void start(String token, String username) {
        Session.token = token;
        Session.username = username;
    }

    public static void end() {
        token = null;
        username = null;
    }

    public static boolean isActive() {
        return token != null;
    }

    public static String getToken() {
        return token;
    }

    public static String getUsername() {
        return username;
    }
}