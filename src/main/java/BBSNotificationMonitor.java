import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Properties;

public class BBSNotificationMonitor {
    // 配置变量
    private static String COOKIES;
    private static String AUTH_TOKEN;
    private static String OUTPUT_FILE;
    private static String NOTIFICATION_API_TEMPLATE;
    private static String NOTIFICATION_USER_ID;
    private static int CHECK_INTERVAL;

    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        // 加载配置文件
        if (!loadConfig()) {
            System.err.println("❌ 配置文件加载失败，程序终止");
            return;
        }

        System.out.println("⏳ 启动消息监控（每" + CHECK_INTERVAL + "秒检查一次）...");
        System.out.println("=".repeat(60));

        while (true) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.printf("\n🕒 检查时间: %s\n", sdf.format(new Date()));

            try {
                // 加载已有消息的时间戳
                Set<Long> existingDatelines = new HashSet<>();
                List<Map<String, Object>> existingMessages = new ArrayList<>();
                loadExistingMessages(existingDatelines, existingMessages);
                boolean isFirstRun = existingDatelines.isEmpty();

                // 获取新数据
                Map<String, Object> data = fetchNotifications();
                if (data == null || !data.containsKey("data")) {
                    System.out.println("⚠️ 获取数据失败，等待下次检查...");
                    TimeUnit.SECONDS.sleep(CHECK_INTERVAL);
                    continue;
                }

                // 处理新消息
                List<Map<String, Object>> newMessages = processMessages(data, existingDatelines);

                // 首次运行处理
                if (isFirstRun) {
                    Map<String, Object> dataData = (Map<String, Object>) data.get("data");
                    if (dataData == null || !dataData.containsKey("rows")) {
                        System.out.println("ℹ️ 首次运行未查询到任何消息");
                    } else {
                        List<Map<String, Object>> rows = (List<Map<String, Object>>) dataData.get("rows");
                        System.out.printf("🆕 首次运行初始化数据（共%d条历史消息）:\n", rows.size());
                        for (Map<String, Object> msg : rows) {
                            printMessage(msg, "");
                        }
                        saveMessages(rows);
                    }
                    TimeUnit.SECONDS.sleep(CHECK_INTERVAL);
                    continue;
                }

                // 常规运行处理
                if (newMessages.isEmpty()) {
                    System.out.println("🔄 本次未发现新消息");
                    TimeUnit.SECONDS.sleep(CHECK_INTERVAL);
                    continue;
                }

                System.out.printf("📨 发现 %d 条新回复：\n", newMessages.size());
                for (Map<String, Object> msg : newMessages) {
                    printMessage(msg, "收到新");
                    sendNotification(getStringValue(msg, "author"), getStringValue(msg, "subject"));
                }

                saveMessages(newMessages);
                System.out.printf("💾 已保存%d条新消息到文件\n", newMessages.size());

                TimeUnit.SECONDS.sleep(CHECK_INTERVAL);
            } catch (Exception e) {
                System.out.println("❌ 发生异常: " + e.getMessage());
                e.printStackTrace();
                try {
                    TimeUnit.SECONDS.sleep(CHECK_INTERVAL);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static boolean loadConfig() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);

            COOKIES = prop.getProperty("bbs.cookies");
            AUTH_TOKEN = prop.getProperty("bbs.auth_token");
            OUTPUT_FILE = prop.getProperty("output.file", "new_messages.json");

            NOTIFICATION_API_TEMPLATE = prop.getProperty("notification.api_template");
            NOTIFICATION_USER_ID = prop.getProperty("notification.user_id");

            CHECK_INTERVAL = Integer.parseInt(prop.getProperty("check.interval.seconds", "15"));

            // 构建完整的通知API URL
            NOTIFICATION_API_TEMPLATE = NOTIFICATION_API_TEMPLATE.replace("{user_id}", NOTIFICATION_USER_ID);

            return true;
        } catch (Exception e) {
            System.err.println("加载配置文件错误: " + e.getMessage());
            return false;
        }
    }

    private static String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private static Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Map<String, Object> fetchNotifications() throws IOException {
        String url = "https://bbs.uestc.edu.cn/star/api/v1/messages/notifications?kind=reply&page=1";

        Headers headers = new Headers.Builder()
                .add("Accept", "application/json, text/plain, */*")
                .add("Referer", "https://bbs.uestc.edu.cn/messages/posts")
                .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36 Edg/139.0.0.0")
                .add("Origin", "https://bbs.uestc.edu.cn")
                .add("X-Requested-With", "XMLHttpRequest")
                .add("Authorization", AUTH_TOKEN)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .addHeader("Cookie", COOKIES)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            }
            return null;
        }
    }

    private static void loadExistingMessages(Set<Long> existingDatelines, List<Map<String, Object>> existingMessages) {
        File file = new File(OUTPUT_FILE);
        if (!file.exists() || file.length() == 0) {
            return;
        }

        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(OUTPUT_FILE));
            List<Map<String, Object>> messages = objectMapper.readValue(jsonData, new TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> msg : messages) {
                Long dateline = getLongValue(msg, "dateline");
                if (dateline != null) {
                    existingDatelines.add(dateline);
                    existingMessages.add(msg);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ 加载已有消息失败: " + e.getMessage());
        }
    }

    private static void printMessage(Map<String, Object> msg, String prefix) {
        System.out.printf("  - 在「%s」帖子中%s%s的回复\n",
                getStringValue(msg, "subject"), prefix, getStringValue(msg, "author"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> processMessages(Map<String, Object> data, Set<Long> existingDatelines) {
        List<Map<String, Object>> newMessages = new ArrayList<>();

        Map<String, Object> dataData = (Map<String, Object>) data.get("data");
        if (dataData == null || !dataData.containsKey("rows")) {
            return newMessages;
        }

        List<Map<String, Object>> rows = (List<Map<String, Object>>) dataData.get("rows");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (Map<String, Object> msg : rows) {
            Long dateline = getLongValue(msg, "dateline");
            if (dateline != null && !existingDatelines.contains(dateline)) {
                Map<String, Object> newMsg = new HashMap<>();
                newMsg.put("id", msg.get("id"));
                newMsg.put("author", msg.get("author"));
                newMsg.put("subject", msg.get("subject"));
                newMsg.put("html_message", msg.get("html_message"));
                newMsg.put("dateline", dateline);
                newMsg.put("fetch_time", sdf.format(new Date()));
                newMessages.add(newMsg);
            }
        }
        return newMessages;
    }

    private static void sendNotification(String author, String subject) {
        String url = NOTIFICATION_API_TEMPLATE.replace("{author}", author).replace("{subject}", subject);

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.printf("  ✉️ 通知内容：您在「%s」帖子中收到「%s」的回复\n", subject, author);
            System.out.printf("    (API状态码: %d)\n", response.code());
        } catch (Exception e) {
            System.out.println("❌ 发送通知失败: " + e.getMessage());
        }
    }

    private static void saveMessages(List<Map<String, Object>> messages) {
        try {
            // 加载已有消息
            Set<Long> existingDatelines = new HashSet<>();
            List<Map<String, Object>> existingMessages = new ArrayList<>();
            loadExistingMessages(existingDatelines, existingMessages);

            // 合并消息并去重
            List<Map<String, Object>> allMessages = new ArrayList<>(existingMessages);
            allMessages.addAll(messages);

            // 使用LinkedHashMap保持插入顺序
            Map<Long, Map<String, Object>> uniqueMessages = new LinkedHashMap<>();
            for (Map<String, Object> msg : allMessages) {
                Long dateline = getLongValue(msg, "dateline");
                if (dateline != null) {
                    uniqueMessages.put(dateline, msg);
                }
            }

            // 写入文件
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(OUTPUT_FILE), new ArrayList<>(uniqueMessages.values()));
        } catch (Exception e) {
            System.out.println("❌ 保存消息失败: " + e.getMessage());
        }
    }
}
