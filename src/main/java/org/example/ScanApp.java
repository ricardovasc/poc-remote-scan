package org.example;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Header;

public class ScanApp {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(corsRule -> {
                    corsRule.anyHost();
                    corsRule.exposeHeader(Header.AUTHORIZATION);
                });
            });
        }).start(8090);

        app.post("/scan", ScanApp::scan);
    }

    private static void scan(Context context) {
        byte[] result = new byte[0];

        context.result(result);
    }
}