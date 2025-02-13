package com.moregrayner.plugins.eventGPTS;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public final class EventGPTS extends JavaPlugin {

    private static EventGPTS instance;
    public static EventGPTS getInstance() {
        return instance;
    }
    private static final Logger LOGGER = Logger.getLogger("마크링크 ver.2.3421");

    public String apiKey = null;


    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        createDataFolder();
        createDefaultConfig();
        FileConfiguration config = getConfig();
        apiKey = config.getString("api-key");
        GPT gpt = new GPT(apiKey);
        Bukkit.getPluginManager().registerEvents(new EventX(gpt), this);
        if (gpt.isApiKeyValid()) {
            LOGGER.info("GPT API 키가 정상적으로 등록되었습니다." + "API Key: " + apiKey);
        } else {
            LOGGER.warning("GPT API 키 등록에 실패했습니다. API 요청이 정상적으로 작동하지 않을 수 있습니다.");
        }
        loadLibrary();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    private void loadLibrary() {
        // libs 폴더의 json 파일 로드
        File libFolder = new File(getDataFolder().getParentFile(), "libs");

        // libs 폴더가 존재하지 않으면 생성
        if (!libFolder.exists()) {
            boolean created = libFolder.mkdirs(); // 폴더 생성 시 성공 여부를 반환
            if (created) {
                LOGGER.info("'libs' 폴더가 생성되었습니다.");
            } else {
                LOGGER.warning("'libs' 폴더 생성에 실패했습니다.");
            }
        }

        File jsonJar = new File(libFolder, "json-20250107.jar");
        if (!jsonJar.exists()) {
            LOGGER.info(jsonJar +" 다운로드를 시작합니다...");
            downloadJsonLibrary(jsonJar);
        }

        if (jsonJar.exists()) {
            try {
                URL jarUrl = jsonJar.toURI().toURL();
                URLClassLoader classLoader = (URLClassLoader) getClass().getClassLoader();

                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);  // private 메서드 접근 허용
                method.invoke(classLoader, jarUrl);

                LOGGER.info("JSON 라이브러리가 성공적으로 로드되었습니다.");
            } catch (Exception e) {
                LOGGER.warning("JSON 라이브러리 로드 중 오류가 발생했습니다: " + e.getMessage());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String error = sw.toString();
                LOGGER.warning("에러 발생!: " + error);
            }
        } else {
            LOGGER.warning("JSON 라이브러리를 'libs' 폴더에서 찾을 수 없습니다.");
            LOGGER.warning("이 플러그인은 JSON 라이브러리를 plugin/libs 폴더에 인스톨시켜줘야 정상적으로 작동합니다.");
        }
    }
    private void createDefaultConfig() {
        // 데이터 폴더 내에 config.yml 경로 지정
        File dataFolder = getDataFolder(); // 플러그인 데이터 폴더
        if (!dataFolder.exists()) { // 데이터 폴더 없을 시 생성
            dataFolder.mkdirs();
            LOGGER.info("플러그인 데이터 폴더가 생성되었습니다.");
        }
        File configFile = new File(dataFolder, "config.yml");

        if (!configFile.exists()) {
            FileConfiguration config = getConfig();
            config.set("api-key", "API 키 - 쌍따옴표로 감싸서 해당 칸에 넣어주세요.");
            saveConfig();

            LOGGER.info("config.yml이 생성되었습니다. 기본값이 구성되었습니다.");
        } else {
            LOGGER.info("config.yml이 이미 존재합니다.");
        }
    }
    private void createDataFolder() {
        File dataFolder = getDataFolder();

        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                LOGGER.info("플러그인 데이터 폴더가 생성되었습니다.");
            } else {
                LOGGER.warning("플러그인 데이터 폴더를 생성하는 데 실패했습니다.");
            }
        } else {
            LOGGER.info("플러그인 데이터 폴더가 이미 존재합니다.");
        }
    }
    private void downloadJsonLibrary(File jsonJar) {
        String urlString = "https://repo1.maven.org/maven2/org/json/json/20250107/json-20250107.jar";
        try {
            URI jarUri = new URI(urlString);
            URL url = jarUri.toURL();

            try (InputStream in = url.openStream()) {
                Files.copy(in, jsonJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("json-20250107.jar 파일이 성공적으로 다운로드되었습니다.");
            }
        } catch (IOException | URISyntaxException e) {
            LOGGER.warning("JSON 라이브러리 다운로드 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
