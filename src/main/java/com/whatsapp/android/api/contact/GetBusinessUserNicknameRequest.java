package com.whatsapp.android.api.contact;

import Message.WhatsMessage;
import ProtocolTree.ProtocolTreeNode;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.contact.GetBusinessUserNicknameResult;
import com.whatsapp.android.request.AbstractRequest;

import java.util.Objects;


/**
 * 获取商业用户粉丝昵称
 *
 * @author sunnoc
 * @date 2022-02-14 15:17
 */
public class GetBusinessUserNicknameRequest extends AbstractRequest<GetBusinessUserNicknameResult> {
    private String userId;

    public GetBusinessUserNicknameRequest(String userId) {
        this.userId = userId;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.GET_BUSINESS_USER_NICKNAME;
    }

    @Override
    public boolean request() {
        user.getGorgeousEngine().getFansNickName(getTaskId(), userId);
        return true;
    }

    @Override
    public GetBusinessUserNicknameResult parseResult(ProtocolTreeNode node) {
        GetBusinessUserNicknameResult checkResult = checkResult(node, GetBusinessUserNicknameResult.class);
        if (checkResult != null) {
            return checkResult;
        }
        ProtocolTreeNode verified_name = node.GetChild("verified_name");
        if (!Objects.isNull(verified_name)) {
            byte[] data = verified_name.GetData();
            if (!Objects.isNull(data)) {
                try {
                    WhatsMessage.Verified vip = WhatsMessage.Verified.parseFrom(data);
                    // long number = vip.getVerifiedOne().getVerifiedOne1();
                    String nickname = vip.getVerifiedOne().getVerifiedOne4();
                    GetBusinessUserNicknameResult userNicknameResult = new GetBusinessUserNicknameResult();
                    userNicknameResult.setNickname(nickname);
                    return userNicknameResult;
                } catch (Exception e) {

                }
            }
        }
        return new GetBusinessUserNicknameResult(StatusResult.fail());
    }
}
