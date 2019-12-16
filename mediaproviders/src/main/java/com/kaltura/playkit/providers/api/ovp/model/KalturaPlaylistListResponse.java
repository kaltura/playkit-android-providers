package com.kaltura.playkit.providers.api.ovp.model;

import com.kaltura.netkit.connect.response.BaseResult;

import java.util.List;

public class KalturaPlaylistListResponse extends BaseResult {
    private List<KalturaPlaylist> objects;
    private Integer totalCount;

    public List<KalturaPlaylist> getObjects() {
        return objects;
    }

    public Integer getTotalCount() {
        return totalCount;
    }
}
