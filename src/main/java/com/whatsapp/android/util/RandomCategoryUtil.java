package com.whatsapp.android.util;

import cn.hutool.core.util.RandomUtil;

public class RandomCategoryUtil {
    private static final String[] CATEGORIES = {
//            "629412378414563",   // 其他商家
            "1223524174334504",    // 汽车服务
            "1086422341396773",    // 服装
            "133436743388217",    // 艺术与娱乐
            "139225689474222",    // 美容、美妆与个人护理
//            "2250",    // 教育
//            "193705277324704",    // 活动策划人
//            "1022050661163852",    // 金融
//            "150108431712141",    // 超市
//            "164243073639257",    // 酒店
//            "145118935550090",    // 医疗与健康
//            "2603",    // 非营利组织
//            "273819889375819",    // 餐馆
//            "200600219953504",    // 零售购物
//            "128232937246338",    // 旅行与交通
    };

    /**
     * 获取随机商业类别
     */
    public static String getRandomCategory() {
        return CATEGORIES[RandomUtil.randomInt(CATEGORIES.length)];
    }
}
