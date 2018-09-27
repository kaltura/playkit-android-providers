package com.kaltura.playkit.providers.api.ovp;

import com.kaltura.netkit.utils.SessionProvider;
import com.kaltura.playkit.providers.api.SimpleSessionProvider;

/**
 * @deprecated Please use {@link SimpleSessionProvider}
 */
@Deprecated
public class SimpleOvpSessionProvider extends SimpleSessionProvider {
    /**
     * Build an OVP {@link SessionProvider} with the specified parameters.
     *
     * @param baseUrl   Kaltura Server URL, such as "https://cdnapisec.kaltura.com".
     * @param partnerId Kaltura partner id.
     * @param ks        Kaltura Session token.
     */
    public SimpleOvpSessionProvider(String baseUrl, int partnerId, String ks) {
        super(baseUrl, partnerId, ks);
    }
}
