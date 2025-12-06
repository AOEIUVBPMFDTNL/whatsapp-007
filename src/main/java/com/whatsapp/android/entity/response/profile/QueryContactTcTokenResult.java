package com.whatsapp.android.entity.response.profile;

import com.whatsapp.android.entity.StatusResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class QueryContactTcTokenResult extends StatusResult {
    private List<TcToken> tcTokens;

    public QueryContactTcTokenResult(List<TcToken> tcTokens) {
        this.tcTokens = tcTokens;
    }
}
