package com.google.cloud.healthcare.fdamystudies.mapper;

import com.google.cloud.healthcare.fdamystudies.beans.AuditLogEventRequest;
import com.google.cloud.healthcare.fdamystudies.common.AuditLogEvent;
import com.google.cloud.healthcare.fdamystudies.common.CommonApplicationPropertyConfig;
import com.google.cloud.healthcare.fdamystudies.common.ErrorCode;
import com.google.cloud.healthcare.fdamystudies.common.MobilePlatform;
import com.google.cloud.healthcare.fdamystudies.common.PlatformComponent;
import com.google.cloud.healthcare.fdamystudies.exceptions.ErrorCodeException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

public final class AuditEventMapper {

  private AuditEventMapper() {}

  private static final String APP_ID = "appId";

  private static final String MOBILE_PLATFORM = "mobilePlatform";

  private static final String CORRELATION_ID = "correlationId";

  private static final String USER_ID = "userId";

  private static final String APP_VERSION = "appVersion";

  private static final String SOURCE = "source";

  public static void addAuditEventHeaderParams(
      HttpHeaders headers, AuditLogEventRequest auditRequest) {
    if (!headers.containsKey(USER_ID)) {
      headers.set(USER_ID, auditRequest.getUserId());
    }
    if (!headers.containsKey(APP_VERSION)) {
      headers.set(APP_VERSION, auditRequest.getAppVersion());
    }
    if (!headers.containsKey(SOURCE)) {
      headers.set(SOURCE, auditRequest.getSource());
    }
    if (!headers.containsKey(CORRELATION_ID)) {
      headers.set(CORRELATION_ID, auditRequest.getCorrelationId());
    }
    if (!headers.containsKey(MOBILE_PLATFORM)) {
      headers.set(MOBILE_PLATFORM, auditRequest.getMobilePlatform());
    }
    if (!headers.containsKey(APP_ID)) {
      headers.set(APP_ID, auditRequest.getAppId());
    }
  }

  public static AuditLogEventRequest fromHttpServletRequest(HttpServletRequest request) {
    AuditLogEventRequest auditRequest = new AuditLogEventRequest();
    auditRequest.setAppId(getValue(request, APP_ID));
    auditRequest.setAppVersion(getValue(request, APP_VERSION));
    auditRequest.setCorrelationId(getValue(request, CORRELATION_ID));
    auditRequest.setUserId(getValue(request, USER_ID));

    String source = getValue(request, SOURCE);
    if (StringUtils.isNotEmpty(source)) {
      PlatformComponent platformComponent = PlatformComponent.fromValue(source);
      if (platformComponent == null) {
        throw new ErrorCodeException(ErrorCode.INVALID_SOURCE_NAME);
      }
      auditRequest.setSource(source);
    }

    auditRequest.setUserIp(getUserIP(request));

    MobilePlatform mobilePlatform = MobilePlatform.fromValue(getValue(request, MOBILE_PLATFORM));
    auditRequest.setMobilePlatform(mobilePlatform.getValue());
    return auditRequest;
  }

  private static String getValue(HttpServletRequest request, String name) {
    String value = request.getHeader(name);
    if (StringUtils.isEmpty(value)) {
      value = getCookieValue(request, name);
    }
    return value;
  }

  private static String getUserIP(HttpServletRequest request) {
    return StringUtils.defaultIfEmpty(
        request.getHeader("X-FORWARDED-FOR"), request.getRemoteAddr());
  }

  private static String getCookieValue(HttpServletRequest req, String cookieName) {
    if (req != null && req.getCookies() != null) {
      return Arrays.stream(req.getCookies())
          .filter(c -> c.getName().equals(cookieName))
          .findFirst()
          .map(Cookie::getValue)
          .orElse(null);
    }
    return null;
  }

  public static AuditLogEventRequest fromAuditLogEventEnumAndCommonPropConfig(
      AuditLogEvent eventEnum,
      CommonApplicationPropertyConfig commonPropConfig,
      AuditLogEventRequest auditRequest) {
    auditRequest.setEventCode(eventEnum.getEventCode());
    // Use enum value where specified, otherwise, use 'source' header value.
    if (eventEnum.getSource().isPresent()) {
      auditRequest.setSource(eventEnum.getSource().get().getValue());
    }

    auditRequest.setDestination(eventEnum.getDestination().getValue());

    if (eventEnum.getUserAccessLevel().isPresent()) {
      auditRequest.setUserAccessLevel(eventEnum.getUserAccessLevel().get().getValue());
    }

    if (eventEnum.getResourceServer().isPresent()) {
      auditRequest.setResourceServer(eventEnum.getResourceServer().get().getValue());
    }
    auditRequest.setSourceApplicationVersion(commonPropConfig.getApplicationVersion());
    auditRequest.setDestinationApplicationVersion(commonPropConfig.getApplicationVersion());
    auditRequest.setPlatformVersion(commonPropConfig.getApplicationVersion());
    auditRequest.setOccured(new Timestamp(Instant.now().toEpochMilli()));
    return auditRequest;
  }
}
