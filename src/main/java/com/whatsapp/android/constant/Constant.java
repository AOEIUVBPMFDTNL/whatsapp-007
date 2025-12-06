package com.whatsapp.android.constant;


import cn.hutool.core.collection.ConcurrentHashSet;
import com.alibaba.fastjson.JSONObject;

import java.util.Map;
import java.util.UUID;

/**
 * @author sunnoc
 * @date 2020-07-23 18:08
 */
public class Constant {
    /**
     * 平台类型
     */
    public static final int PLATFORM_TYPE = 3;
    /**
     * 密匙key
     */
    public static final String KEY = UUID.randomUUID().toString();
    /**
     * 通讯录筛选开通key
     */
    public static final String SYNC_CONTACT_KEY = "e134df04-d743-4d37-b752-72eadb47fb8c";
    /**
     * 终端前缀
     */
    public static final String TERMINAL_PREFIX = "WHATSAPP-";
    /**
     * jar包路径
     */
    public static final String JAR_PATH = "/usr/javawork/whatsapp/";
    /**
     * jar包名称
     */
    public static final String JAR_PACK_NAME = "whatsapp-android-0.0.1-SNAPSHOT.jar";
    /**
     * 更新jar包名称
     */
    public static final String UPDATE_JAR_PACK_NAME = "update-starter-1.0-SNAPSHOT.jar";
    /**
     * ip白名单列表
     */
    public static final ConcurrentHashSet<String> IP_WHITE_LIST = new ConcurrentHashSet<>();

    /**
     * 空异常
     */
    public static final int NULL_EXCEPTION = 99999;
    /**
     * http请求异常
     */
    public static final int HTTP_REQUEST_EXCEPTION = 99998;
    /**
     * cdn下载地址
     */
    public static final String CDN_DOWNLOAD_ADDR = "http://wa-register.zc.lu";
    /**
     * 成功
     */
    public static final String OK = "ok";
    /**
     * 失败
     */
    public static final String FAIL = "fail";
    /**
     * 用户信息key
     */
    public static final String USER_INFO_KEY = "whatsApp:userInfo";
    /**
     * 防止瞎更新key
     */
    public static final String UPDATE_TERMINAL_KEY = "whatsApp:ntkj";
    /**
     * 下载地址key
     */
    public static final String UPDATE_TERMINAL_URL_KEY = "whatsApp:1234567";
    /**
     * 记录重试三次还失败的小号
     */
    public static final String USERNAME_HAS_RETRY_KEY = "whatsApp:has_retry:%s";
    /**
     * 记录重试三次还失败的小号
     */
    public static final String USERNAME_TIME_KEY = "whatsapp:time_key";
    /**
     * whatsapp环境链接加密key
     */
    public static final String ENV_ENCRYPT_KEY = "whatsApp:x123456";
    /**
     * WhatsApp获取通信key
     */
    public static final String GET_SECRET_KEY = "whatsApp:getSecretKey";
    /**
     * whatsapp发送记录
     */
    public static final String WA_SEND_RECORD = "wa:send:";
    /**
     * 南田消息标签
     */
    public static final String NT_MSG_TAG = "NT:";
    /**
     * 新南田消息标签
     */
    public static final String NT_MSG_TAG_NEW = "L1N2T3K4JN";
    /**
     * 养号标签
     */
    public static final String NT_MSG_TAG_FEED = "TKXT";

    /**
     * 需要转发养号
     */
    public static final String ZF_MSG_TAG_FEED = "TLTS";
    /**
     * ios msgId prefix
     */
    public static final String IOS_MSG_PREFIX = "3A";


    /**
     * bucket名称
     */
    public static final String BUCKET_NAME = "nanshan-fb-1257892306";

    /**
     * 密钥
     */
    public static final String ACCESS_KEY = "AKIDvcttdD8rrlRdZFRmriY89vo3LPJImjdI";

    /**
     * 密钥
     */
    public static final String SECRET_KEY = "aYIvx2rPi7ICq2GFySrIUzE1atvp289t";

    /**
     * 地域
     */
    public static final String REGION = "ap-singapore";
    /**
     * 任务标记
     */
    public static final String TASK_TAG = "taskTag";
    /**
     * status code
     */
    public static final Map<String, Object> STATUS_CODE = initStatusCode();

    public static final String ANDROID = "android";
    public static final String IOS = "ios";
    public static final int GCM_RETRY_LIMIT = 3;
    public static final int GCM_LOGIN_AUTH_TAG = 2;
    public static final int MCS_HEARTBEAT_PING_TAG = 0;
    public static final int MCS_MSG_ACK = 7;

    public static final int MCS_CLOSE_TAG = 4;
    public static final int MCS_DATA_MESSAGE_STANZA_TAG = 8;
    public static final String GCM_API_KEY = "AIzaSyCGOJbGQ95SWrXxl8wk-_cRQZcJl42bvDU";
    public static final String GCM_ACK_URL = "https://android.googleapis.com/gcm/send";

    public static final String OPEN_IP_POOL_AGENT = "whatsapp:open_agentIp";

    public static final String OPEN_SYSTEM_IP_AGENT = "whatsapp:open_system_agentIp";

    public static GcmProxyConstant GCM_INFO;

    public static class OnlineStatus {
        /**
         * 掉线重登
         */
        public static final String RE_LOGIN = "账号掉线重登中";
        /**
         * 离线
         */
        public static final String OFFLINE = "需要先登陆或账号离线中";
        /**
         * 用户离线
         */
        public static final String USER_OFFLINE = "用户不在线";
        /**
         * 登陆中退出失败
         */
        public static final String LOGGING_EXIT_FAIL = "退出失败，账号正在登陆中";
        /**
         * 已在线
         */
        public static final String USER_ONLINE = "用户已在线，请勿重复登录";
        /**
         * 用户登陆中
         */
        public static final String USER_LOGGING = "当前账号正在登录中";
        /**
         * 封号
         */
        public static final String KILL = "封号";
        /**
         * 环境失效
         */
        public static final String ENV_FAILURE = "环境失效";
        /**
         * 版本过低
         */
        public static final String LOW_VERSION = "版本过低";
        /**
         * wa 官方限制登陆
         */
        public static final String WA_LIMIT_LOGIN = "WS官方限制登录";
        /**
         * 动态ip格式错误
         */
        public static final String DYNAMIC_IP_FORMAT_ERROR = "动态ip规则错误";
        /**
         * 上传密钥失败
         */
        public static final String UPLOAD_KEY_FAIL = "账号需修复后再试";
        public static final String GCM_DISCONNECT = "账号退出，请重登";
        /**
         * WA服务器错误，请重试
         */
        public static final String WA_SERVER_ERROR = "WA服务器错误，请重试";
    }

    public static class ExceptionReason {
        /**
         * 粉丝离线时间太长
         */
        public static final String FANS_OFFLINE_TOO_LONG = "未获取到粉丝信息,可能粉丝很久没上过线";
    }

    private static Map<String, Object> initStatusCode() {
        String content = "{\"400\":\"请求无效\",\"401\":\"未授权\",\"402\":\"商家资格 — 支付问题\",\"403\":\"禁止访问\",\"404\":\"未找到\",\"405\":\"不得使用此方法，请稍后再试\",\"408\":\"消息无效\",\"410\":\"消息已过期\",\"412\":\"先决条件失效\",\"420\":\"消息受流量限制\",\"429\":\"达到流量上限\",\"430\":\"未签名的证书\",\"432\":\"证书编号不一致\",\"433\":\"证书签名无效\",\"470\":\"重新互动消息\",\"471\":\"达到垃圾消息流量上限\",\"472\":\"用户的号码作实验用途\",\"480\":\"用户可能发生更改\",\"500\":\"一般性错误\",\"501\":\"目前不支持此消息类型。\",\"503\":\"服务器错误\",\"504\":\"超时\",\"1000\":\"一般性错误\",\"1001\":\"消息过长\",\"1002\":\"收信人类型无效\",\"1004\":\"资源已经存在\",\"1005\":\"访问遭拒\",\"1006\":\"未找到资源\",\"1007\":\"收信人受到封锁，无法接收消息\",\"1008\":\"必要参数缺失\",\"1009\":\"参数值无效\",\"1010\":\"不需要某个参数\",\"1011\":\"服务未就绪\",\"1013\":\"用户无效\",\"1014\":\"内部错误\",\"1015\":\"请求过多\",\"1016\":\"系统过载\",\"1017\":\"非主 Master\",\"1018\":\"非主要核心应用\",\"1021\":\"用户有误\",\"1022\":\"未配置 Webhooks 网址\",\"1023\":\"发生数据库错误\",\"1024\":\"需要更改密码\",\"1025\":\"无效请求\",\"1026\":\"接收者无法接收\",\"1028\":\"系统通知需要确认\",\"2000\":\"模板参数数量不一致\",\"2001\":\"缺少模板\",\"2002\":\"模板获取失败\",\"2003\":\"缺少模板包\",\"2004\":\"模板参数长度过长\",\"2005\":\"模板中的已填充文字过长\",\"2006\":\"违反模板空格政策\",\"2007\":\"违反模板格式字符政策\",\"2008\":\"不支持模板的媒体格式\",\"2009\":\"缺少模板所需组件\",\"2010\":\"模板中的已填充网址无效\",\"2011\":\"模板电话号码无效\",\"2012\":\"模板参数格式不一致\",\"2013\":\"不支持模板按钮\",\"2015\":\"版块数量无效\",\"2016\":\"行数无效\",\"2017\":\"违反字符政策\",\"2023\":\"商品数量无效\",\"2024\":\"找不到目录编号\",\"2025\":\"目录编号未关联 API 编号\",\"2026\":\"缺少商品\",\"2027\":\"找不到任何商品\",\"2028\":\"列出的所有商品不合规\",\"2029\":\"列出的部分商品不合规\",\"2030\":\"列出的混合商品无效且商品不合规\",\"2036\":\"无效标头结构\",\"2050\":\"缺少合规信息\"}";
        return JSONObject.parseObject(content);
    }
}
