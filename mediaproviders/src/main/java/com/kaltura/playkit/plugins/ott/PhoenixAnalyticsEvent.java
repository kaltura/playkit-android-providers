/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins.ott;

import com.kaltura.playkit.PKEvent;

/**
 * Created by anton.afanasiev on 27/03/2017.
 */

public class PhoenixAnalyticsEvent implements PKEvent {

    public final PhoenixAnalyticsEvent.Type type;

    public PhoenixAnalyticsEvent(PhoenixAnalyticsEvent.Type type) {
        this.type = type;
    }
    public enum Type {
        REPORT_SENT,
        CONCURRENCY_ERROR,
        BOOKMARK_ERROR,
        ERROR
    }

    public static class PhoenixAnalyticsReport extends PhoenixAnalyticsEvent {

        public final String reportedEventName;

        public PhoenixAnalyticsReport(String reportedEventName) {
            super(Type.REPORT_SENT);
            this.reportedEventName = reportedEventName;
        }
    }

    public static class ErrorEvent extends PhoenixAnalyticsEvent {

        public final int errorCode;
        public final String errorMessage;

        public ErrorEvent(PhoenixAnalyticsEvent.Type errorType, int errorCode, String errorMessage) {
            super(errorType);
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }

    public static class BookmarkErrorEvent extends ErrorEvent {

        public BookmarkErrorEvent(int errorCode, String errorMessage) {
            super(Type.BOOKMARK_ERROR, errorCode, errorMessage);
        }
    }

    public static class ConcurrencyErrorEvent extends ErrorEvent {

        public ConcurrencyErrorEvent(int errorCode, String errorMessage) {
            super(Type.CONCURRENCY_ERROR, errorCode, errorMessage);
        }
    }

    @Override
    public Enum eventType() {
        return this.type;
    }
}
