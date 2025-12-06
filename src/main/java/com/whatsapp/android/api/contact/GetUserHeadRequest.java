package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Util.StringUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.contact.GetUserHeadResult;
import com.whatsapp.android.request.AbstractRequest;
import com.whatsapp.android.util.KeyLockUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 获取用户头像
 *
 * @author sunnoc
 * @date 2021-03-10 15:36
 */
@Slf4j
public class GetUserHeadRequest extends AbstractRequest<GetUserHeadResult> {
    private String userId;
    private boolean preview;

    public GetUserHeadRequest(String userId, boolean preview) {
        this.userId = userId;
        this.preview = preview;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_USER_HEAD_IMAGE;
    }

    @Override
    public boolean request() {
        String username = user.getLoginPack().getUsername();
        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(userId);
        byte[] token = null;
        try {
            KeyLockUtil.lock(username);
            token = user.getGorgeousEngine().axolotlManager_.trustedContactStore.getToken(jidInfo.recipientId);
        } catch (Exception ignored) {
        } finally {
            KeyLockUtil.unlock(username);
        }
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", getTaskId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:profile:picture"));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        iq.AddAttribute(new StanzaAttribute("target", user.getGorgeousEngine().JidNormalize(userId)));
        /*ProtocolTreeNode picture = new ProtocolTreeNode("picture");
        picture.AddAttribute(new StanzaAttribute("type", "image"));*/
        ProtocolTreeNode picture = new ProtocolTreeNode("picture");
        if (!preview) {
            picture.AddAttribute(new StanzaAttribute("type", "image")); // image preview代表预览图
            picture.AddAttribute(new StanzaAttribute("query", "url"));
        } else {
            picture.AddAttribute(new StanzaAttribute("type", "preview"));
        }

        if (token != null) {
            ProtocolTreeNode tcToken = new ProtocolTreeNode("tctoken");
            tcToken.SetData(token);
            picture.AddChild(tcToken);
        }
        iq.AddChild(picture);
        user.getGorgeousEngine().AddTask("GetHDHead", iq);
        return true;
    }

    @Override
    public boolean showLogs() {
        return false;
    }

    @Override
    public GetUserHeadResult parseResult(ProtocolTreeNode node) {
        GetUserHeadResult checkResult = checkResult(node, GetUserHeadResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        ProtocolTreeNode pictureNode = node.getOneChildren("picture");
        if (pictureNode == null) {
            ProtocolTreeNode errorNode = node.getOneChildren("error");
            if (errorNode != null) {
                String code = errorNode.GetAttributeValue("code");
                if ("401".equals(code)) {
                    return new GetUserHeadResult(StatusResult.fail("获取头像失败"), true);
                }
            }
            return new GetUserHeadResult(StatusResult.fail("获取头像失败"));
        }
        String t = pictureNode.GetAttributeValue("id");
        long modifyTime = Convert.toLong(t, 0L);
        GetUserHeadResult getUserHeadResult = new GetUserHeadResult();
        if (modifyTime != 0) {
            getUserHeadResult.setModifyPictureTime(DateUtil.date(modifyTime * 1000).toString());
        }
        String url = pictureNode.GetAttributeValue("url");
        getUserHeadResult.setPicture(url);
        if (((preview || StrUtil.isEmpty(url)) && pictureNode.GetData() != null)) {
            // 若拿不到url或者获取调用接口是取缩略图且可以拿到base64则回传base64
            getUserHeadResult.setPicture(Base64.encode(pictureNode.GetData()));
        }
        return getUserHeadResult;
    }
}
