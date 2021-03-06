/*
 * Copyright 2016 Artem Labazin <xxlabaza@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.xxlabaza.feign.form;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import feign.Response;
import feign.jackson.JacksonEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static feign.Logger.Level.FULL;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 30.04.2016
 */
public class TestClient {

    private static ConfigurableApplicationContext context;

    private static final TestApi api;

    static {
        api = Feign.builder()
                .encoder(new FormEncoder(new JacksonEncoder()))
                .logger(new feign.Logger.JavaLogger().appendToFile("log.txt"))
                .logLevel(FULL)
                .target(TestApi.class, "http://localhost:8080");
    }

    @BeforeClass
    public static void beforeClass () {
        context = SpringApplication.run(Server.class);
    }

    @AfterClass
    public static void afterClass () {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void testForm () {
        Response response = api.form("1", "1");

        Assert.assertNotNull(response);
        Assert.assertEquals(200, response.status());
    }

    @Test
    public void testFormException () {
        Response response = api.form("1", "2");

        Assert.assertNotNull(response);
        Assert.assertEquals(400, response.status());
    }

    @Test
    public void testUpload () throws Exception {
        Path path = Paths.get(this.getClass().getClassLoader().getResource("file.txt").toURI());
        Assert.assertTrue(Files.exists(path));

        String response = api.upload(10, Boolean.TRUE, path);
        Assert.assertEquals(Files.size(path), Long.parseLong(response));
    }

    @Test
    public void testJson () {
        Dto dto = new Dto("Artem", 11);
        String response = api.json(dto);

        Assert.assertEquals("ok", response);
    }

    @Test
    public void testQueryMap () {
        Map<String, Object> value = Collections.singletonMap("filter", Arrays.asList("one", "two", "three", "four"));

        String response = api.queryMap(value);

        Assert.assertEquals("4", response);
    }

    interface TestApi {

        @RequestLine("POST /form")
        @Headers("Content-Type: application/x-www-form-urlencoded")
        Response form (@Param("key1") String key1, @Param("key2") String key2);

        @RequestLine("POST /upload/{id}")
        @Headers("Content-Type: multipart/form-data")
        String upload (@Param("id") Integer id, @Param("public") Boolean isPublic, @Param("file") Path file);

        @RequestLine("POST /json")
        @Headers("Content-Type: application/json")
        String json (Dto dto);

        @RequestLine("POST /query_map")
        String queryMap (@QueryMap Map<String, Object> value);
    }
}
