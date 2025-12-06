package com.whatsapp.android.constant;

/**
 * @author sunnoc
 * @date 2020-07-28 11:52
 */
public class TypeConstant {
    public static final String NOTIFY = "notify";
    public static final String TASK = "task";
    /**
     * 刷新ip白名单
     */
    public static final String REFRESH_IP_WHITE_LIST = "refreshIpWhiteList";
    /**
     * 任务
     */
    public static final String WM_TASK = "wmTask";
    /**
     * 异步通知
     */
    public static final String CALLBACK = "callback";
    public static final String INITIALIZE = "initialize";
    public static final String PING = "ping";

    public static class NotifyType {
        /**
         * 账号退出事件
         */
        public static final String ON_EXIT = "onExit";
        /**
         * 账号登录成功通知
         */
        public static final String ON_LOGIN_SUCCESS = "onLoginSuccess";
        /**
         * 登录失败通知
         */
        public static final String ON_LOGIN_FAIL = "onLoginFail";
        /**
         * cookie存在更新通知
         */
        public static final String ON_COOKIE_UPDATE = "onCookieUpdate";
        /**
         * fb mqtt async message
         */
        public static final String ASYNC_MESSAGE = "asyncMessage";
        /**
         * 群聊异步消息
         */
        public static final String GROUP_ASYNC_MESSAGE = "groupAsyncMessage";


    }

    public static class TaskType {
        /**
         * 获取终端实际在线账号列表
         */
        public static final String GET_TERMINAL_RECORD_KEY = "getTerminalRecordKey";
        /**
         * 更新终端
         */
        public static final String UPDATE_TERMINAL = "updateTerminal";

        /**
         * 更新websocket链接地址
         */
        public static final String UPDATE_WEBSOCKET_ADDR = "updateWebsocketAddr";

        /**
         * 登录
         */
        public static final String LOGIN = "login";
        /**
         * 账号退出
         */
        public static final String LOGOUT = "logout";
        public static final String DISCONNECT = "disconnect";
        /**
         * 发送图片消息
         */
        public static final String SEND_IMAGE_MESSAGE = "sendImageMessage";
        /**
         * 发送文字消息
         */
        public static final String SEND_TEXT_MESSAGE = "sendTextMessage";
        /**
         * 发送图文链接超链信息
         */
        public static final String SEND_HYPER_LINK_TEXT_MESSAGE = "sendHyperLinkTextMessage";
        /**
         * 发送名片消息
         */
        public static final String SEND_VCARD_MESSAGE = "sendVCardMessage";
        /**
         * 发送语音消息
         */
        public static final String SEND_VOICE_MESSAGE = "sendVoiceMessage";
        /**
         * 发送视频消息
         */
        public static final String SEND_VIDEO_MESSAGE = "sendVideoMessage";
        /**
         * 发送文件消息
         */
        public static final String SEND_FILE_MESSAGE = "sendFileMessage";
        /**
         * 发送位置消息
         */
        public static final String SEND_LOCATION_MESSAGE = "sendLocationMessage";
        /**
         * 撤回消息
         */
        public static final String REVOKE_MESSAGE = "revokeMessage";
        /**
         * 撤回心情消息
         */
        public static final String REVOKE_REACTION_MESSAGE = "revokeReactionMessage";
        /**
         * 消息回复表情
         */
        public static final String SEND_REACTION_MESSAGE = "sendReactionMessage";
        /**
         * 标记消息为已读
         */
        public static final String MARK_READ = "markRead";
        /**
         * 创建小组
         */
        public static final String CREATE_GROUP = "createGroup";
        /**
         * 修改群主题
         */
        public static final String MODIFY_GROUP_SUBJECT = "modifyGroupSubject";
        /**
         * 邀请群成员
         */
        public static final String INVITE_GROUP_MEMBERS = "inviteGroupMembers";
        /**
         * 移除群成员
         */
        public static final String REMOVE_GROUP_MEMBERS = "removeGroupMembers";
        /**
         * 设置管理员
         */
        public static final String SET_GROUP_ADMIN = "setGroupAdmin";
        /**
         * 离开群
         */
        public static final String LEAVE_GROUP = "leaveGroup";
        /**
         * 审批进群请求
         */
        public static final String APPROVE_MEMBER_JOIN_GROUP = "approveMemberJoinGroup";
        /**
         * 获取群信息
         */
        public static final String GET_GROUP_INFO = "getGroupInfo";
        /**
         * 通过群链接进群
         */
        public static final String ACCEPT_INVITE_TO_GROUP = "acceptInviteToGroup";
        /**
         * 获取群邀请链接
         */
        public static final String GET_GROUP_INVITE_LINK = "getGroupInviteLink";
        /**
         * 重置群邀请链接
         */
        public static final String RESET_GROUP_INVITE_LINK = "resetGroupInviteLink";
        /**
         * 同步通讯录校验开通
         */
        public static final String SYNC_CONTACT = "syncContact";
        /**
         * 查询WsId
         */
        public static final String QUERY_CONTACT = "queryContact";
        /**
         * 添加通讯录
         */
        public static final String ADD_CONTACT = "addContact";
        /**
         * 查询用户在线时间
         */
        public static final String SUBSCRIBE = "subscribe";
        /**
         * 取消订阅
         */
        public static final String UN_SUBSCRIBE = "unSubscribe";
        /**
         * 设置昵称
         */
        public static final String SET_NAME = "setName";
        /**
         * 设置状态
         */
        public static final String SET_STATUS = "setStatus";
        /**
         * 修改头像
         */
        public static final String MODIFY_HEAD_IMAGE = "modifyHeadImage";
        /**
         * 获取用户头像
         */
        public static final String GET_USER_HEAD_IMAGE = "getUserHeadImage";
        /**
         * 获取二维码
         */
        public static final String GET_QR_CODE = "getQrCode";
        /**
         * 通过二维码获取信息
         */
        public static final String GET_CONTACT_BY_CODE = "getContactByCode";
        /**
         * 校验号并发送验证码
         */
        public static final String CHECK_PHONE_EXIST = "checkPhoneExist";
        /**
         * 校验手机号是否封号
         */
        public static final String CHECK_PHONE_BLOCKED = "checkPhoneBlocked";
        /**
         * 发送验证码
         */
        public static final String SEND_SMS = "sendSms";
        /**
         * 提交注册
         */
        public static final String SUBMIT_REGISTER = "submitRegister";
        /**
         * 发送资源消息
         */
        public static final String SEND_RESOURCES_MESSAGE = "sendResourcesMessage";
        /**
         * 上传资源文件
         */
        public static final String UPLOAD_RESOURCES = "uploadResources";
        /**
         * 二次验证
         */
        public static final String SECOND_AUTH = "secondAuth";
        /**
         * 修改群描述
         */
        public static final String MODIFY_GROUP_DESC = "modifyGroupDesc";
        /**
         * 发送输入消息
         */
        public static final String SEND_TYPING_MESSAGE = "sendTypingMessage";
        /**
         * 发送文本动态
         */
        public static final String SEND_MOMENT_TEXT = "sendMomentText";
        /**
         * 修改群发言权限
         */
        public static final String MODIFY_GROUP_SEND_MSG_PERMISSION = "modifyGroupSendMsgPermission";
        /**
         * 修改编辑群组信息权限
         */
        public static final String MODIFY_GROUP_EDIT_PERMISSION = "modifyGroupEditPermission";
        /**
         * 获取用户设备信息
         */
        public static final String GET_USER_DEVICE_INFO = "getUserDeviceInfo";
        /**
         * 限时消息设置
         */
        public static final String SET_TIME_LIMITED_MESSAGE = "setTimeLimitedMessage";
        /**
         * 信任联系人
         */
        public static final String TRUSTED_CONTACT = "trustedContact";
        /**
         * 从邀请链接获取群信息
         */
        public static final String GET_GROUP_INFO_FROM_INVITE_LINK = "getGroupInfoFromInviteLink";
        /**
         * 获取用户状态
         */
        public static final String GET_USER_STATUS = "getUserStatus";
        /**
         * 过滤粉丝信息
         */
        public static final String FILTER_FANS_INFO_REQUEST = "filterFansInfoRequest";
        /**
         * 扫码web whatsapp
         */
        public static final String SCAN_WEB_WHATSAPP = "scanWebWhatsapp";
        /**
         * 移除多设备
         */
        public static final String REMOVE_COMPANION_DEVICE = "removeCompanionDevice";
        /**
         * 发送视频语音通话
         */
        public static final String SEND_VOIP_MESSAGE = "sendVoipMessage";
        /**
         * 发送带语音文件的语音通话
         */
        public static final String SEND_VOIP_WITH_VOICE_FILE_MESSAGE = "sendVoipWithVoiceFileMessage";
        /**
         * 接听语音通话
         */
        public static final String ACCEPT_VOIP_CALL = "acceptVoipCall";
        /**
         * 获取商业用户粉丝昵称
         */
        public static final String GET_BUSINESS_USER_NICKNAME = "getBusinessUserNickname";
        /**
         * 重置gcm
         */
        public static final String RESET_GCM = "resetGcm";
        /**
         * 移除多设备表信息
         */
        public static final String REMOVE_MORE_DEVICE_TABLE_INFO = "removeMoreDeviceTableInfo";
        /**
         * 生成消息id
         */
        public static final String GENERATE_MSG_ID = "generateMsgId";
        /**
         * 强制刷新群密钥
         */
        public static final String FORCE_REFRESH_GROUP_KEYS = "forceRefreshGroupKeys";
        /**
         * 批量发送消息
         */
        public static final String BATCH_SEND_MESSAGE = "batchSendMessage";
        /**
         * 批量发送等待中的消息
         */
        public static final String BATCH_SEND_FAKE_SESSION_MESSAGE = "batchSendFakeSessionMessage";
        /**
         * 激活抢登
         */
        public static final String ACTIVATE_FORCE_LOGIN = "activateForceLogin";
        /**
         * 获取强制登录状态
         */
        public static final String GET_FORCE_LOGIN_STATUS = "getForceLoginStatus";
        /**
         * 获取隐私设置
         */
        public static final String GET_PRIVACY_SETTINGS = "getPrivacySettings";
        /**
         * 更新已读设置
         */
        public static final String UPDATE_READ_RECEIPTS_PRIVACY_SETTING = "updateReadReceiptsPrivacySetting";
        /**
         * 更新头像隐私设置
         */
        public static final String UPDATE_PROFILE_PRIVACY_SETTING = "updateProfilePrivacySetting";
        /**
         * 更新发送状态隐私设置
         */
        public static final String UPDATE_STATUS_PRIVACY_SETTING = "updateStatusPrivacySetting";
        /**
         * 更新在线状态隐私设置
         */
        public static final String UPDATE_ONLINE_PRIVACY_SETTING = "updateOnlinePrivacySetting";
        /**
         * 更新最近上线时间状态隐私设置
         */
        public static final String UPDATE_LAST_PRIVACY_SETTING = "updateLastPrivacySetting";
        /**
         * 更新群组邀请隐私设置
         */
        public static final String UPDATE_GROUP_ADD_PRIVACY_SETTING = "updateGroupAddSetting";
        /**
         * 更新语音视频邀请隐私设置
         */
        public static final String UPDATE_CALL_ADD_PRIVACY_SETTING = "updateCallAddSetting";
        /**
         * 修改群成员邀请权限
         */
        public static final String MODIFY_GROUP_ADD_PERMISSION = "modifyGroupAddPermission";
        /**
         * 修改是否开启管理员审批新成员进群功能
         */
        public static final String MODIFY_GROUP_MEMBERSHIP_APPROVAL_MODE = "modifyGroupMembershipApprovalMode";
        /**
         * 获取小号下所有的群组关系
         */
        public static final String GET_ALL_GROUP_RELATIONSHIP = "getAllGroupRelationship";
        /**
         * 获取群组所有待审批请求
         */
        public static final String GET_GROUP_MEMBERSHIP_APPROVAL = "getGroupMembershipApproval";
        /**
         * 获取社群下的子群列表
         */
        public static final String GET_SUB_GROUPS = "getSubGroups";
        /**
         * 账号重注册
         */
        public static final String ACCOUNT_RE_REGISTRATION = "accountReRegistration";
        /**
         * 发送voip终止
         */
        public static final String SEND_VOIP_TERMINATE = "sendVoipTerminate";
        /**
         * 删除头像
         */
        public static final String DELETE_HEAD_IMAGE = "deleteHeadImage";
        /**
         * 同步通讯录校验开通完整信息
         */
        public static final String SYNC_CONTACT_ALL_INFO = "syncContactAllInfo";
        /**
         * 查询联系人tcToken
         */
        public static final String QUERY_CONTACT_TC_TOKEN = "queryContactTcToken";
        /**
         * 导入联系人tcToken
         */
        public static final String IMPORT_CONTACT_TC_TOKEN = "importContactTcToken";
        /**
         * 查询联系人校验开通完整信息
         */
        public static final String QUERY_CONTACT_ALL_INFO = "queryContactAllInfo";
    }

    public static class MsgType {
        /**
         * 语音通话邀请
         */
        public static final String VOICE_CALL_INVITE = "voiceCallInvite";
        /**
         * 语音/视频邀请结束
         */
        public static final String VOICE_CALL_END = "voiceCallEnd";
        /**
         * 语音/视频邀请拒绝
         */
        public static final String VOICE_CALL_REJECT = "voiceCallReject";
        /**
         * 文字消息
         */
        public static final String TEXT_MSG = "textMsg";
        /**
         * 图片消息
         */
        public static final String IMAGE_MSG = "imageMsg";
        /**
         * 动画消息
         */
        public static final String GIF_MSG = "gifMsg";
        /**
         * 语音消息
         */
        public static final String VOICE_MSG = "voiceMsg";
        /**
         * 视屏消息
         */
        public static final String VIDEO_MSG = "videoMsg";
        /**
         * 文件消息
         */
        public static final String FILE_MSG = "fileMsg";
        /**
         * 地理位置消息
         */
        public static final String LOCATION_MSG = "locationMsg";
        /**
         * 名片消息
         */
        public static final String CONTACT_CARD_MSG = "contactCardMsg";
        /**
         * 名片数组消息
         */
        public static final String CONTACT_CARD_ARRAY_MSG = "contactCardArrayMsg";
        /**
         * 接收消息已读取
         */
        public static final String RECEIPT_MSG_READ = "receiptMsgRead";
        /**
         * 贴纸消息
         */
        public static final String STICKER_MSG = "stickerMsg";
        /**
         * lottie贴纸消息
         */
        public static final String LOTTIE_STICKER_MSG = "lottieStickerMsg";
        /**
         * 群通知消息
         */
        public static final String GROUP_NOTIFY_MSG = "groupNotifyMsg";
        /**
         * 收到voip呼叫
         */
        public static final String VOIP_CALL_RECEIVED = "voipCallReceived";
        /**
         * 主动创建voip呼叫
         */
        public static final String VOIP_CALL_CREATE = "voipCallCreate";
        /**
         * voip呼叫已连接
         */
        public static final String VOIP_CALL_CONNECTED = "voipCallConnected";
        /**
         * 联系人隐私令牌
         */
        public static final String CONTACTS_PRIVACY_TOKEN = "contactsPrivacyToken";
    }

}
