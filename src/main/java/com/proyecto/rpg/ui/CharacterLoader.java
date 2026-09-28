package com.proyecto.rpg.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lee characters.json de forma tolerante, sin depender de las clases del modelo. */
public class CharacterLoader {

    public static class Info {
        public String name = "Sin nombre";
        public String race = "?";
        public String characterClass = "?";
        public List<String> items = new ArrayList<>();
        public List<String> skills = new ArrayList<>();
        public Map<String, String> stats = new LinkedHashMap<>();

        @Override
        public String toString() {
            return name + " (" + race + " / " + characterClass + ")";
        }
    }

    private static final String[] CANDIDATES = {
            "src/main/resources/data/characters.json",
            "data/characters.json",
            "characters.json"
    };

    public static Path findDefault() {
        for (String c : CANDIDATES) {
            Path p = Path.of(c);
            if (Files.exists(p)) return p;
        }
        return null;
    }

    public static List<Info> load(Path path) throws IOException {
        JsonElement root = JsonParser.parseString(Files.readString(path));
        JsonArray arr = new JsonArray();
        if (root.isJsonArray()) {
            arr = root.getAsJsonArray();
        } else if (root.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().entrySet()) {
                if (e.getValue().isJsonArray()) { arr = e.getValue().getAsJsonArray(); break; }
            }
        }
        List<Info> out = new ArrayList<>();
        for (JsonElement el : arr) {
            if (el.isJsonObject()) out.add(parse(el.getAsJsonObject()));
        }
        return out;
    }

    private static Info parse(JsonObject o) {
        Info i = new Info();
        String n = text(o, "name", "nombre");
        if (n != null) i.name = n;
        String r = text(o, "race", "raza");
        if (r != null) i.race = r;
        String c = text(o, "characterClass", "class", "clazz", "clase");
        if (c != null) i.characterClass = c;

        for (String k : new String[]{"outfit", "outfits", "items", "inventory", "equipment"}) {
            collect(o, k, i.items);
        }
        collect(o, "skills", i.skills);

        for (String k : new String[]{"stats", "finalStats", "baseStats"}) {
            if (o.has(k) && o.get(k).isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject(k).entrySet()) {
                    if (e.getValue().isJsonPrimitive()) i.stats.put(e.getKey(), e.getValue().getAsString());
                }
                break;
            }
        }
        return i;
    }

    private static void collect(JsonObject o, String key, List<String> target) {
        if (!o.has(key) || o.get(key).isJsonNull()) return;
        JsonElement e = o.get(key);
        if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) target.add(label(x));
        } else {
            target.add(label(e));
        }
    }

    private static String text(JsonObject o, String... keys) {
        for (String k : keys) {
            if (o.has(k) && !o.get(k).isJsonNull()) return label(o.get(k));
        }
        return null;
    }

    private static String label(JsonElement e) {
        if (e.isJsonPrimitive()) return e.getAsString();
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            for (String k : new String[]{"name", "nombre", "title"}) {
                if (o.has(k) && o.get(k).isJsonPrimitive()) return o.get(k).getAsString();
            }
        }
        return e.toString();
    }
}