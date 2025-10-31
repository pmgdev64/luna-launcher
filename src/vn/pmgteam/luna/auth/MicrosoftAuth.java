package vn.pmgteam.luna.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.Desktop;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

public class MicrosoftAuth {

    private static final String CLIENT_ID = "YOUR_CLIENT_ID"; // app MS
    private static final String AUTH_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String SCOPE = "XboxLive.signin offline_access";

    private String authCode;
    private String accessToken;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public MinecraftSession loginMinecraft() throws Exception {
        // 1. OAuth Code Flow
        String code = loginMicrosoft();
        // 2. Exchange authCode -> access_token
        exchangeCodeForToken(code);
        // 3. Xbox Live auth
        String xbl = xboxLiveAuth(accessToken);
        // 4. XSTS
        String xsts = xstsAuth(xbl);
        // 5. Minecraft login
        String mcToken = minecraftLogin(xsts);
        // 6. Profile
        return minecraftProfile(mcToken);
    }

    private String loginMicrosoft() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        String redirectUri = "http://localhost:" + port + "/callback";

        server.createContext("/callback", (HttpExchange exchange) -> {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("code=")) {
                authCode = query.split("code=")[1].split("&")[0];
                String response = "<h2>Login OK! Close this window.</h2>";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); }
                latch.countDown();
            }
        });
        server.start();

        String url = AUTH_URL + "?" +
                "client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8);

        Desktop.getDesktop().browse(new URI(url));
        latch.await();
        server.stop(1);
        return authCode;
    }

    private void exchangeCodeForToken(String code) throws Exception {
        String body = "client_id=" + CLIENT_ID +
                "&grant_type=authorization_code" +
                "&code=" + code +
                "&redirect_uri=http://localhost" + // ⚠️ chú ý phải khớp redirect_uri
                "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        String resp = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
        JsonNode json = mapper.readTree(resp);
        accessToken = json.get("access_token").asText();
    }

    private String xboxLiveAuth(String accessToken) throws Exception {
        String body = """
        {
          "Properties": {
            "AuthMethod": "RPS",
            "SiteName": "user.auth.xboxlive.com",
            "RpsTicket": "d=%s"
          },
          "RelyingParty": "http://auth.xboxlive.com",
          "TokenType": "JWT"
        }
        """.formatted(accessToken);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        String resp = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
        JsonNode json = mapper.readTree(resp);
        return json.get("Token").asText();
    }

    private String xstsAuth(String xblToken) throws Exception {
        String body = """
        {
          "Properties": {
            "SandboxId": "RETAIL",
            "UserTokens": ["%s"]
          },
          "RelyingParty": "rp://api.minecraftservices.com/",
          "TokenType": "JWT"
        }
        """.formatted(xblToken);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        String resp = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
        JsonNode json = mapper.readTree(resp);
        return json.get("Token").asText();
    }

    private String minecraftLogin(String xstsToken) throws Exception {
        String body = """
        {
          "identityToken": "XBL3.0 x=%s;%s"
        }
        """.formatted("userhash", xstsToken); // TODO: lấy userhash từ xbl response

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        String resp = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
        JsonNode json = mapper.readTree(resp);
        return json.get("access_token").asText();
    }

    private MinecraftSession minecraftProfile(String mcToken) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + mcToken)
                .GET()
                .build();
        String resp = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
        JsonNode json = mapper.readTree(resp);
        return new MinecraftSession(
                json.get("id").asText(),
                json.get("name").asText(),
                mcToken
        );
    }

    // Session model
    public record MinecraftSession(String uuid, String name, String token) {}
}
