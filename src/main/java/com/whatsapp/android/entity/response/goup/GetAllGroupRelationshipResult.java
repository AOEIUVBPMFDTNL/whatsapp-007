package com.whatsapp.android.entity.response.goup;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;

import java.util.List;

/**
 * TODO 写明类的作用
 *
 * @author Rocky
 */
@Data
public class GetAllGroupRelationshipResult extends StatusResult {
    private List<CreateGroupResult> groups;

    public GetAllGroupRelationshipResult() {
    }

    public GetAllGroupRelationshipResult(List<CreateGroupResult> groups) {
        this.groups = groups;
    }

    public GetAllGroupRelationshipResult(StatusResult statusResult) {
        super(statusResult.getStatus(), statusResult.getMessage());
    }

}
