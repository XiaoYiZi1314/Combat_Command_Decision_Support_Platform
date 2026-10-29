import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 旧数据附件直传 COS 工具（一次性迁移用）。
 *
 * 用途：把「无法归位到单个业务对象」的旧附件（当前只有 132 个水源档案 PDF）
 *       上传到腾讯云 COS，但不写入 ccds_file_object（其 biz_id 语义要求指向具体
 *       水源/重点单位，而旧版档案是按「站+类型/地址」聚合的，无稳定外键）。
 *       上传后由迁移报告与清单保留归位线索，供人工核对。
 *
 * 清单格式与 LegacyObjectImportService 消费的一致：{"items":[{cosKey,localPath,contentType,sizeBytes,...}]}
 *
 * 幂等：COS 侧已存在同 key 对象时跳过（doesObjectExist），重复执行不重复上传。
 */
public final class CosBulkUpload {

    /**
     * 直传单文件上限。COS 单次 PUT 支持 5GB，这里取 512MB 留足余量。
     */
    private static final long MAX_DIRECT_UPLOAD_BYTES = 512L * 1024L * 1024L;

    /** 清单项字段。 */
    private static final class Item {
        public String cosKey;
        public String localPath;
        public String contentType;
        public long sizeBytes;
        public String originalName;
        public String stationName;
    }

    private CosBulkUpload() {
    }

    /**
     * 主入口。
     *
     * @param args args[0] 清单路径；args[1..] 透传给 COS 的额外过滤（可选，暂未使用）
     * @throws Exception 上传失败时抛出
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("用法: CosBulkUpload <manifest.json>");
            System.exit(2);
        }
        String region = requireEnv("CCDS_COS_REGION");
        String bucket = requireEnv("CCDS_COS_BUCKET");
        String secretId = requireEnv("CCDS_COS_SECRET_ID");
        String secretKey = requireEnv("CCDS_COS_SECRET_KEY");
        String manifestPath = args[0];

        String text = new String(java.nio.file.Files.readAllBytes(new File(manifestPath).toPath()), StandardCharsets.UTF_8);
        List<Item> items = parseItems(text);
        System.out.println("清单项：" + items.size());

        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        COSClient client = new COSClient(cred, clientConfig);
        try {
            int uploaded = 0;
            int skipped = 0;
            int failed = 0;
            List<String> failures = new ArrayList<String>();
            for (Item item : items) {
                File file = new File(item.localPath);
                if (!file.isFile()) {
                    failed++;
                    failures.add(item.originalName + "：本地文件不存在");
                    continue;
                }
                // 20MB 是应用层「用户上传」的单文件上限（FileRuleConstant.MAX_SIZE_BYTES）。
                // 本工具走 COS 直传，不经应用上传接口，故对归档文件不施加该限制；
                // 真正的限制是 COS 单次 PUT 的大小，26MB 量级远在其内。
                if (item.sizeBytes > MAX_DIRECT_UPLOAD_BYTES) {
                    failed++;
                    failures.add(item.originalName + "：超过直传上限");
                    continue;
                }
                try {
                    if (client.doesObjectExist(bucket, item.cosKey)) {
                        skipped++;
                        continue;
                    }
                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(item.sizeBytes);
                    metadata.setContentType(item.contentType);
                    try (InputStream in = new FileInputStream(file)) {
                        PutObjectRequest request = new PutObjectRequest(bucket, item.cosKey, in, metadata);
                        client.putObject(request);
                    }
                    uploaded++;
                    if ((uploaded + skipped) % 20 == 0) {
                        System.out.println("  已处理 " + (uploaded + skipped) + "/" + items.size());
                    }
                } catch (RuntimeException ex) {
                    failed++;
                    failures.add(item.originalName + "：" + ex.getClass().getSimpleName());
                }
            }
            System.out.println("上传完成：上传=" + uploaded + " 已存在跳过=" + skipped + " 失败=" + failed);
            for (String failure : failures) {
                System.out.println("  失败：" + failure);
            }
        } finally {
            client.shutdown();
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少环境变量 " + name);
        }
        return value;
    }

    /**
     * 从清单文本中取出 items 数组。清单由迁移脚本生成，字段名固定，
     * 这里用正则按字段名解析，避免为一次性工具引入 JSON 依赖。
     *
     * @param text 清单全文
     * @return 解析出的清单项
     */
    private static List<Item> parseItems(String text) {
        List<Item> items = new ArrayList<Item>();
        int index = 0;
        while (true) {
            // 清单由 json.dump(indent=1) 生成，字段顺序固定；以 bizType 作为一项的起点，
            // 保证能读到 cosKey 之前的 originalName / stationName 等字段。
            int start = text.indexOf("\"bizType\"", index);
            if (start < 0) {
                break;
            }
            int end = text.indexOf("}", start);
            if (end < 0) {
                break;
            }
            String block = text.substring(start, end);
            Item item = new Item();
            item.cosKey = readString(block, "cosKey");
            item.localPath = readString(block, "localPath");
            item.contentType = readString(block, "contentType");
            item.originalName = readString(block, "originalName");
            item.stationName = readString(block, "stationName");
            item.sizeBytes = readLong(block, "sizeBytes");
            if (item.cosKey != null && item.localPath != null) {
                items.add(item);
            }
            index = end + 1;
        }
        return items;
    }

    private static String readString(String block, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(block);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replace("\\\\\"", "\"").replace("\\\\\\\\", "\\\\");
    }

    private static long readLong(String block, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)").matcher(block);
        if (!matcher.find()) {
            return 0L;
        }
        return Long.parseLong(matcher.group(1));
    }
}
