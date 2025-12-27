package com.lotm.lotm.client.util;

import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LOTM 模糊搜索助手
 * <p>
 * 职责：提供高级搜索匹配功能。
 * 兼容性：自动检测并挂钩 JEI, JustEnoughCharacters, Searchables 等模组的拼音/模糊匹配逻辑。
 * 如果未安装这些模组，回退到标准的 String.contains()。
 */
public class LotMFuzzySearchHelper {

    private static volatile boolean initialized = false;
    private static Method activeMethod = null;

    // 已知的搜索增强 Mod 映射
    // 格式: ModID -> [ClassName, MethodName, Param1Type, Param2Type]
    private static final Map<String, String[]> KNOWN_MODS = new LinkedHashMap<>();

    static {
        // JustEnoughCharacters (拼音搜索)
        KNOWN_MODS.put("jecharacters", new String[]{
                "me.towdium.jecharacters.utils.Match", "contains", "String", "CharSequence"
        });
        // Searchables
        KNOWN_MODS.put("searchables", new String[]{
                "com.supermartijn642.searchables.Searchables", "matches", "String", "String"
        });
    }

    /**
     * 判断文本是否匹配查询词
     *
     * @param text  源文本 (例如技能名称)
     * @param query 查询词 (用户输入)
     * @return true 如果匹配
     */
    public static boolean matches(String text, String query) {
        ensureInitialized();

        if (text == null || query == null || query.isEmpty()) {
            return true;
        }

        // 1. 尝试使用 Mod 提供的增强搜索 (拼音等)
        if (activeMethod != null) {
            try {
                Object result = activeMethod.invoke(null, text.toLowerCase(), query.toLowerCase());
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            } catch (Exception ignored) {
                // 调用失败，静默回退
            }
        }

        // 2. 回退：标准包含匹配 (忽略大小写)
        return text.toLowerCase().contains(query.toLowerCase());
    }

    private static synchronized void ensureInitialized() {
        if (initialized) return;
        initialized = true;

        for (Map.Entry<String, String[]> entry : KNOWN_MODS.entrySet()) {
            String modId = entry.getKey();
            if (ModList.get().isLoaded(modId)) {
                String[] info = entry.getValue();
                if (tryLoadMethod(info[0], info[1], info[2], info[3])) {
                    break; // 找到一个可用的就停止
                }
            }
        }
    }

    private static boolean tryLoadMethod(String className, String methodName, String p1Type, String p2Type) {
        try {
            Class<?> clazz = Class.forName(className);
            Class<?> c1 = p1Type.equals("CharSequence") ? CharSequence.class : String.class;
            Class<?> c2 = p2Type.equals("CharSequence") ? CharSequence.class : String.class;
            activeMethod = clazz.getMethod(methodName, c1, c2);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
