/*
 * MIT License
 *
 * Copyright (c) 2018 Olivia Zoe
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.lunarwatcher.java.haileybot.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * Utility parser class for jackson parsers. Also really easy to extend with different data formats; if Jackson
 * supports it, this can parse it by using a different Factory.
 */
public class JacksonParser {
    private static final JacksonParser jsonParser = new JacksonParser(new JsonFactory());

    private final JsonFactory factory;
    private final ObjectMapper mapper;

    private JacksonParser(JsonFactory factory) {
        this.factory = factory;
        mapper = new ObjectMapper(factory);

    }

    @NotNull
    public Map<String, Object> parse(String data) throws IOException {
        JsonNode root = mapper.readTree(data);
        return unpackTree(root);
    }

    @NotNull
    public Map<String, Object> parse(InputStream stream) throws IOException {
        JsonNode root = mapper.readTree(stream);
        return unpackTree(root);
    }

    public void saveData(Map<String, Object> data, BufferedWriter writer, boolean prettyPrint) throws IOException {
        JsonGenerator generator = factory.createGenerator(writer);
        if(prettyPrint){
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
        }
        generator.writeStartObject();
        for (Map.Entry<String, Object> entry : data.entrySet()){
            generator.writeFieldName(entry.getKey());
            packTree(generator, entry.getValue());
        }
        generator.writeEndObject();

        generator.close();
    }

    private void packTree(JsonGenerator generator, Object value) throws IOException{
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            generator.writeStartObject();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String fieldName = entry.getKey().toString();
                Object v = entry.getValue();

                generator.writeFieldName(fieldName);
                packTree(generator, v);
            }

            generator.writeEndObject();
            return;
        }else if (value instanceof Collection) {
            Collection<?> list = (Collection<?>) value;
            generator.writeStartArray();

            for (Object item : list) {
                packTree(generator, item);
            }

            generator.writeEndArray();
            return;
        }else if (value instanceof Integer) {
            generator.writeNumber((Integer) value);
            return;
        }else if (value instanceof Long) {
            generator.writeNumber((Long) value);
            return;
        }else if(value instanceof Boolean) {
            generator.writeBoolean((Boolean) value);
            return;
        }else if (value == null) {
            generator.writeNull();
            return;
        }else if(value instanceof Double){
            generator.writeNumber((Double) value);
            return;
        }else if(value instanceof Float){
            generator.writeNumber((Float) value);
            return;
        }else if(value instanceof Serializable){
            generator.writeObject(value);
            return;
        }

        generator.writeString(value.toString());

    }

    private Map<String, Object> unpackTree(JsonNode root){
        if(root == null)
            return new HashMap<>();

        Iterator<String> it = root.fieldNames();
        Map<String, Object> cache = new HashMap<>();

        if(it == null){
            return cache;
        }

        while(it.hasNext()){
            String fieldName = it.next();
            JsonNode node = root.get(fieldName);
            Object value = parseNode(node);

            if(fieldName != null && value != null)
                cache.put(fieldName, value);
        }

        return cache;
    }

    private Object parseNode(JsonNode node){
        if(node.isArray()){
            List<Object> list = new ArrayList<>();
            Iterator<JsonNode> it = node.elements();
            while(it.hasNext()){
                JsonNode item = it.next();
                Object parsedItem = parseNode(item);
                list.add(parsedItem);
            }
            return list;
        }else if(node.isObject()) {
            Map<String, Object> map = new HashMap<>();
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode field = node.get(fieldName);
                Object parsed = parseNode(field);
                map.put(fieldName, parsed);
            }
            return map;
        }else if(node.isInt())
            return node.asInt();
        else if(node.isLong())
            return node.asLong();
        else if(node.isDouble())
            return node.doubleValue();
        else if(node.isFloat())
            return node.floatValue();
        else if(node.isBigDecimal())
            return new BigDecimal(node.asText());
        else if(node.isBigInteger())
            return node.bigIntegerValue();
        else if(node.isBoolean())
            return node.asBoolean();

        return node.asText();
    }

    public static JacksonParser getJsonParser() {
        return jsonParser;
    }
    public JsonFactory getFactory(){
        return factory;
    }


}

