package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.contact.FilterFansInfoResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.WhatsAppUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 获取用上传preKey密钥时间
 *
 * @author sunnoc
 * @date 2022-02-14 16:47
 */
public class FilterFansInfoRequest extends AbstractRequest<FilterFansInfoResult> {
    public FilterFansInfoRequest(List<String> userIds) {
        this.userIds = userIds;
    }

    private List<String> userIds;

    @Override
    public String funcName() {
        return TypeConstant.TaskType.FILTER_FANS_INFO_REQUEST;
    }

    @Override
    public boolean request() {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("xmlns", "encrypt"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));

        ProtocolTreeNode key = new ProtocolTreeNode("key");
        for (String jid : userIds) {
            ProtocolTreeNode user = new ProtocolTreeNode("user");
            user.AddAttribute(new StanzaAttribute("jid", WhatsAppUtils.JidNormalize(jid)));
            key.AddChild(user);
        }
        iq.AddChild(key);
        user.getGorgeousEngine().AddTask("getLastOnlineTime", iq);
        return true;
    }

    @Override
    public long timeOut() {
        return 60L;
    }

    @Override
    public boolean showLogs() {
        return false;
    }

    @Override
    public FilterFansInfoResult parseResult(ProtocolTreeNode node) {
        FilterFansInfoResult checkResult = checkResult(node, FilterFansInfoResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        LinkedList<ProtocolTreeNode> listNode = node.GetChildren("list");
        if (listNode.isEmpty()) {
            return new FilterFansInfoResult(StatusResult.fail("获取失败"));
        }
        LinkedList<ProtocolTreeNode> users = listNode.get(0).GetChildren("user");
        FilterFansInfoResult filterFansInfoResult = new FilterFansInfoResult();
        List<FilterFansInfoResult.FansInfo> fansInfos = new ArrayList<>();
        for (ProtocolTreeNode user : users) {
            FilterFansInfoResult.FansInfo fansInfo = new FilterFansInfoResult.FansInfo();
            try {
                //pre key
                ProtocolTreeNode preKeyNode = user.GetChild("key");
                if (null == preKeyNode) {
                    //账号在服务器没有密钥了，太久没活跃了
                    fansInfo.setActive(false);
                } else {
                    fansInfo.setActive(true);
                    String t = user.GetAttributeValue("t");
                    long modifyTime = Convert.toLong(t, 0L);
                    if (modifyTime != 0) {
                        fansInfo.setTime(DateUtil.date(modifyTime * 1000).toString());
                    }
                }
                String jid = user.GetAttributeValue("jid");
                fansInfo.setUserId(jid);
                fansInfos.add(fansInfo);
            } catch (Exception e) {
                return new FilterFansInfoResult(StatusResult.fail("获取异常"));
            }
        }
        filterFansInfoResult.setList(fansInfos);
        return filterFansInfoResult;
    }
}
