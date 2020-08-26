/*
 * Copyright 2020 Google LLC
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package com.google.cloud.healthcare.fdamystudies.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.healthcare.fdamystudies.bean.StudyMetadataBean;
import com.google.cloud.healthcare.fdamystudies.beans.NotificationBean;
import com.google.cloud.healthcare.fdamystudies.beans.NotificationForm;
import com.google.cloud.healthcare.fdamystudies.common.BaseMockIT;
import com.google.cloud.healthcare.fdamystudies.dao.CommonDaoImpl;
import com.google.cloud.healthcare.fdamystudies.repository.AppPermissionRepository;
import com.google.cloud.healthcare.fdamystudies.repository.StudyPermissionRepository;
import com.google.cloud.healthcare.fdamystudies.repository.UserRegAdminUserRepository;
import com.google.cloud.healthcare.fdamystudies.service.StudiesServices;
import com.google.cloud.healthcare.fdamystudies.testutils.Constants;
import com.google.cloud.healthcare.fdamystudies.testutils.TestUtils;
import com.google.cloud.healthcare.fdamystudies.usermgmt.model.AppPermission;
import com.google.cloud.healthcare.fdamystudies.usermgmt.model.StudyInfoBO;
import com.google.cloud.healthcare.fdamystudies.usermgmt.model.StudyPermission;
import com.google.cloud.healthcare.fdamystudies.usermgmt.model.UserRegAdminUser;
import com.google.cloud.healthcare.fdamystudies.util.ErrorCode;
import com.google.cloud.healthcare.fdamystudies.util.Permission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public class StudiesControllerTest extends BaseMockIT {

  private static final String STUDY_METADATA_PATH = "/myStudiesUserMgmtWS/studies/studymetadata";

  private static final String SEND_NOTIFICATION_PATH =
      "/myStudiesUserMgmtWS/studies/sendNotification";

  @Autowired private StudiesController studiesController;

  @Autowired private StudiesServices studiesServices;

  @Autowired private CommonDaoImpl commonDao;

  @Autowired private AppPermissionRepository appPermissionRepository;

  @Autowired private StudyPermissionRepository studyPermissionRepository;

  @Autowired private UserRegAdminUserRepository userRegAdminUserRepository;

  @Autowired private ObjectMapper objectMapper;

  @Test
  public void contextLoads() {
    assertNotNull(studiesController);
    assertNotNull(mockMvc);
    assertNotNull(studiesServices);
    assertNotNull(commonDao);
  }

  public StudyMetadataBean createStudyMetadataBean() {
    return new StudyMetadataBean(
        Constants.STUDY_ID,
        Constants.STUDY_TITLE,
        Constants.STUDY_VERSION,
        Constants.STUDY_TYPE,
        Constants.STUDY_STATUS,
        Constants.STUDY_CATEGORY,
        Constants.STUDY_TAGLINE,
        Constants.STUDY_SPONSOR,
        Constants.STUDY_ENROLLING,
        Constants.APP_ID_VALUE,
        Constants.APP_NAME,
        Constants.APP_DESCRIPTION,
        Constants.ORG_ID_VALUE);
  }

  @Test
  public void addUpdateStudyMetadataSuccess() throws Exception {
    HttpHeaders headers = TestUtils.getCommonHeaders();
    String requestJson = getObjectMapper().writeValueAsString(createStudyMetadataBean());

    mockMvc
        .perform(
            post(STUDY_METADATA_PATH)
                .content(requestJson)
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(String.valueOf(HttpStatus.OK.value()))));

    HashSet<String> set = new HashSet<>();
    set.add(Constants.STUDY_ID);
    List<StudyInfoBO> list = commonDao.getStudyInfoSet(set);
    StudyInfoBO studyInfoBo =
        list.stream()
            .filter(x -> x.getCustomId().equals(Constants.STUDY_ID))
            .findFirst()
            .orElse(null);
    assertNotNull(studyInfoBo);
    assertEquals(Constants.STUDY_SPONSOR, studyInfoBo.getSponsor());
    assertEquals(Constants.STUDY_TAGLINE, studyInfoBo.getTagline());
  }

  @Test
  public void addUpdateStudyMetadataSuccessForPermissions() throws Exception {
    HttpHeaders headers = TestUtils.getCommonHeaders();
    StudyMetadataBean studyMetaDataBean = createStudyMetadataBean();
    studyMetaDataBean.setStudyId(Constants.NEW_STUDY_ID);
    studyMetaDataBean.setAppId(Constants.NEW_APP_ID_VALUE);
    String requestJson = getObjectMapper().writeValueAsString(studyMetaDataBean);
    performPost(
        STUDY_METADATA_PATH, requestJson, headers, String.valueOf(HttpStatus.OK.value()), OK);

    List<AppPermission> appPermissionList = appPermissionRepository.findAll();
    List<StudyPermission> studyPermissionList = studyPermissionRepository.findAll();
    List<UserRegAdminUser> userRegAdminUserList = userRegAdminUserRepository.findAll();

    AppPermission appPermission =
        appPermissionList
            .stream()
            .filter(x -> x.getAppInfo().getAppId().equals(Constants.NEW_APP_ID_VALUE))
            .findFirst()
            .orElse(null);
    StudyPermission studyPermission =
        studyPermissionList
            .stream()
            .filter(x -> x.getStudyInfo().getCustomId().equals(Constants.NEW_STUDY_ID))
            .findFirst()
            .orElse(null);
    UserRegAdminUser userRegAdminUser =
        userRegAdminUserList
            .stream()
            .filter(x -> x.getSuperAdmin().equals(true))
            .findFirst()
            .orElse(null);

    assertNotNull(userRegAdminUser);
    assertNotNull(appPermission);
    assertNotNull(appPermission.getUrAdminUser());
    assertEquals(Constants.NEW_APP_ID_VALUE, appPermission.getAppInfo().getAppId());
    assertEquals(Permission.READ_EDIT.value(), appPermission.getEdit());
    assertEquals(appPermission.getCreatedBy(), userRegAdminUser.getId());

    assertNotNull(studyPermission);
    assertNotNull(studyPermission.getUrAdminUser());
    assertEquals(Constants.NEW_STUDY_ID, studyPermission.getStudyInfo().getCustomId());
    assertEquals(Permission.READ_EDIT.value(), studyPermission.getEdit());
    assertEquals(studyPermission.getCreatedBy(), userRegAdminUser.getId());
  }

  @Test
  public void addUpdateStudyMetadataBadRequest() throws Exception {

    HttpHeaders headers = TestUtils.getCommonHeaders();

    // without studyId
    StudyMetadataBean metadataBean = createStudyMetadataBean();
    metadataBean.setStudyId("");
    String requestJson = getObjectMapper().writeValueAsString(metadataBean);
    mockMvc
        .perform(
            post(STUDY_METADATA_PATH)
                .content(requestJson)
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isBadRequest());

    // without studyVersion
    metadataBean = createStudyMetadataBean();
    metadataBean.setStudyVersion("");
    requestJson = getObjectMapper().writeValueAsString(metadataBean);
    mockMvc
        .perform(
            post(STUDY_METADATA_PATH)
                .content(requestJson)
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isBadRequest());

    // without appId
    metadataBean = createStudyMetadataBean();
    metadataBean.setAppId("");
    requestJson = getObjectMapper().writeValueAsString(metadataBean);
    mockMvc
        .perform(
            post(STUDY_METADATA_PATH)
                .content(requestJson)
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isBadRequest());

    // without orgId
    metadataBean = createStudyMetadataBean();
    metadataBean.setOrgId("");
    requestJson = getObjectMapper().writeValueAsString(metadataBean);
    mockMvc
        .perform(
            post(STUDY_METADATA_PATH)
                .content(requestJson)
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void sendNotificationBadRequest() throws Exception {

    HttpHeaders headers =
        TestUtils.getCommonHeaders(Constants.CLIENT_ID_HEADER, Constants.SECRET_KEY_HEADER);

    // null body
    NotificationForm notificationForm = null;
    String requestJson = getObjectMapper().writeValueAsString(notificationForm);
    mockMvc
        .perform(
            post(SEND_NOTIFICATION_PATH)
                .content(requestJson)
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isBadRequest());

    // empty notificationType
    requestJson =
        getNotificationForm(
            Constants.STUDY_ID, Constants.CUSTOM_STUDY_ID, Constants.APP_ID_HEADER, "");
    mockMvc
        .perform(
            post(SEND_NOTIFICATION_PATH)
                .content(requestJson)
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  @Disabled
  // TODO(#668) Remove @Disabled when Github test case failed issue fix
  public void sendNotificationSuccess() throws Exception {
    HttpHeaders headers =
        TestUtils.getCommonHeaders(Constants.CLIENT_ID_HEADER, Constants.SECRET_KEY_HEADER);

    // StudyLevel notificationType
    String requestJson =
        getNotificationForm(
            Constants.STUDY_ID,
            Constants.CUSTOM_STUDY_ID,
            Constants.APP_ID_VALUE,
            Constants.STUDY_LEVEL);

    mockMvc
        .perform(
            post(SEND_NOTIFICATION_PATH)
                .content(requestJson)
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is(ErrorCode.EC_200.errorMessage())))
        .andExpect(jsonPath("$.code", is(ErrorCode.EC_200.code())))
        .andExpect(jsonPath("$.response.multicast_id", greaterThan(0L)))
        .andExpect(
            jsonPath(
                "$.response.results[0].message_id", is("0:1491324495516461%31bd1c9631bd1c96")));
    // GatewayLevel notificationType
    requestJson =
        getNotificationForm(
            Constants.STUDY_ID,
            Constants.CUSTOM_STUDY_ID,
            Constants.APP_ID_VALUE,
            Constants.GATEWAY_LEVEL);

    mockMvc
        .perform(
            post(SEND_NOTIFICATION_PATH)
                .content(requestJson)
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is(ErrorCode.EC_200.errorMessage())))
        .andExpect(jsonPath("$.code", is(ErrorCode.EC_200.code())))
        .andExpect(jsonPath("$.response.multicast_id", greaterThan(0L)))
        .andExpect(
            jsonPath(
                "$.response.results[0].message_id", is("0:1491324495516461%31bd1c9631bd1c96")));
  }

  private String getNotificationForm(
      String studyId, String customStudyId, String appId, String notificationType)
      throws JsonProcessingException {

    NotificationBean notificationBean = null;
    notificationBean = new NotificationBean(studyId, customStudyId, appId, notificationType);
    List<NotificationBean> list = new ArrayList<NotificationBean>();
    list.add(notificationBean);
    NotificationForm notificationForm = new NotificationForm(list);
    return getObjectMapper().writeValueAsString(notificationForm);
  }

  protected ObjectMapper getObjectMapper() {
    return objectMapper;
  }
}
