package com.recomind.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.recomind.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writes the standard {success, message, data} JSON envelope and reads JSON request bodies. */
public final class ApiResponse {
    private static final Gson GSON = new Gson();

    private ApiResponse() { }

    public static void send(HttpServletResponse resp, int status, boolean success, String message, Object data)
            throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", success);
        body.put("message", message);
        body.put("data", data);
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        PrintWriter writer = resp.getWriter();
        writer.write(GSON.toJson(body));
        writer.flush();
    }

    public static void ok(HttpServletResponse resp, String message, Object data) throws IOException {
        send(resp, HttpServletResponse.SC_OK, true, message, data);
    }

    public static void error(HttpServletResponse resp, int status, String message) throws IOException {
        send(resp, status, false, message, null);
    }

    /** Parses the request body as a JSON object (an empty body gives an empty object). */
    public static JsonObject readBody(HttpServletRequest req) throws IOException {
        try (BufferedReader reader = req.getReader()) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || element.isJsonNull()) {
                return new JsonObject();
            }
            if (!element.isJsonObject()) {
                throw new ValidationException("A JSON object is expected");
            }
            return element.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new ValidationException("Invalid JSON body");
        }
    }

    public static String str(JsonObject body, String key) {
        JsonElement e = body.get(key);
        if (e == null || e.isJsonNull()) {
            return null;
        }
        try {
            return e.getAsString();
        } catch (RuntimeException ex) {
            throw new ValidationException(key + " must be text");
        }
    }

    public static Double num(JsonObject body, String key) {
        JsonElement e = body.get(key);
        if (e == null || e.isJsonNull()) {
            return null;
        }
        try {
            return e.getAsDouble();
        } catch (RuntimeException ex) {
            throw new ValidationException(key + " must be a number");
        }
    }
}