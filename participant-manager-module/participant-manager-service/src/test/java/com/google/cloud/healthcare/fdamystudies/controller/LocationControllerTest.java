/*
 * Copyright 2020 Google LLC
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package com.google.cloud.healthcare.fdamystudies.controller;

import static com.google.cloud.healthcare.fdamystudies.common.CommonConstants.ACTIVE_STATUS;
import static com.google.cloud.healthcare.fdamystudies.common.CommonConstants.INACTIVE_STATUS;
import static com.google.cloud.healthcare.fdamystudies.common.CommonConstants.YES;
import static com.google.cloud.healthcare.fdamystudies.common.JsonUtils.readJsonFile;
import static com.google.cloud.healthcare.fdamystudies.common.TestConstants.CUSTOM_ID_VALUE;
import static com.google.cloud.healthcare.fdamystudies.common.TestConstants.LOCATION_DESCRIPTION_VALUE;
import static com.google.cloud.healthcare.fdamystudies.common.TestConstants.LOCATION_NAME_VALUE;
import static com.google.cloud.healthcare.fdamystudies.common.TestConstants.UPDATE_LOCATION_DESCRIPTION_VALUE;
import static com.google.cloud.healthcare.fdamystudies.common.TestConstants.UPDATE_LOCATION_NAME_VALUE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.cloud.healthcare.fdamystudies.beans.LocationRequest;
import com.google.cloud.healthcare.fdamystudies.beans.UpdateLocationRequest;
import com.google.cloud.healthcare.fdamystudies.common.ApiEndpoint;
import com.google.cloud.healthcare.fdamystudies.common.BaseMockIT;
import com.google.cloud.healthcare.fdamystudies.common.CommonConstants;
import com.google.cloud.healthcare.fdamystudies.common.ErrorCode;
import com.google.cloud.healthcare.fdamystudies.common.JsonUtils;
import com.google.cloud.healthcare.fdamystudies.common.MessageCode;
import com.google.cloud.healthcare.fdamystudies.common.Permission;
import com.google.cloud.healthcare.fdamystudies.helper.TestDataHelper;
import com.google.cloud.healthcare.fdamystudies.model.LocationEntity;
import com.google.cloud.healthcare.fdamystudies.model.UserRegAdminEntity;
import com.google.cloud.healthcare.fdamystudies.repository.LocationRepository;
import com.google.cloud.healthcare.fdamystudies.repository.UserRegAdminRepository;
import com.google.cloud.healthcare.fdamystudies.service.LocationService;
import com.jayway.jsonpath.JsonPath;

public class LocationControllerTest extends BaseMockIT {

  @Autowired private LocationController controller;

  @Autowired private LocationService locationService;

  @Autowired private TestDataHelper testDataHelper;

  @Autowired private LocationRepository locationRepository;

  @Autowired private UserRegAdminRepository userRegAdminRepository;

  private UserRegAdminEntity userRegAdminEntity;

  private LocationEntity locationEntity;

  protected MvcResult result;

  @BeforeEach
  public void setUp() {
    userRegAdminEntity = testDataHelper.createUserRegAdmin();
    locationEntity = testDataHelper.createLocation();
  }

  @Test
  public void contextLoads() {
    assertNotNull(controller);
    assertNotNull(mockMvc);
    assertNotNull(locationService);
  }

  @Test
  public void shouldReturnBadRequestForAddNewLocation() throws Exception {
    HttpHeaders headers = newCommonHeaders();
    LocationRequest locationRequest = new LocationRequest();
    result =
        mockMvc
            .perform(
                post(ApiEndpoint.ADD_NEW_LOCATION.getPath())
                    .content(JsonUtils.asJsonString(locationRequest))
                    .headers(headers)
                    .contextPath(getContextPath()))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.violations").isArray())
            .andReturn();

    String actualResponse = result.getResponse().getContentAsString();
    String expectedResponse = readJsonFile("/responses/add_location_bad_request.json");
    JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
  }

  @Test
  public void shouldReturnForbiddenForLocationAccessDenied() throws Exception {

    userRegAdminEntity.setManageLocations(Permission.READ_VIEW.value());
    userRegAdminRepository.saveAndFlush(userRegAdminEntity);
    HttpHeaders headers = newCommonHeaders();

    mockMvc
        .perform(
            post(ApiEndpoint.ADD_NEW_LOCATION.getPath())
                .content(JsonUtils.asJsonString(getLocationRequest()))
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.error_description", is(ErrorCode.LOCATION_ACCESS_DENIED.getDescription())))
        .andReturn();
  }

  @Test
  public void shouldCreateANewLocation() throws Exception {
    HttpHeaders headers = newCommonHeaders();
    // Step 1: Call API to create new location
    result =
        mockMvc
            .perform(
                post(ApiEndpoint.ADD_NEW_LOCATION.getPath())
                    .content(JsonUtils.asJsonString(getLocationRequest()))
                    .headers(headers)
                    .contextPath(getContextPath()))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.locationId", notNullValue()))
            .andExpect(jsonPath("$.message", is(MessageCode.ADD_LOCATION_SUCCESS.getMessage())))
            .andReturn();

    String locationId = JsonPath.read(result.getResponse().getContentAsString(), "$.locationId");

    // Step 2: verify saved values
    Optional<LocationEntity> optLocationEntity = locationRepository.findById(locationId);
    LocationEntity locationEntity = optLocationEntity.get();
    assertNotNull(locationEntity);
    assertEquals(CUSTOM_ID_VALUE, locationEntity.getCustomId());
    assertEquals(LOCATION_NAME_VALUE, locationEntity.getName());
    assertEquals(LOCATION_DESCRIPTION_VALUE, locationEntity.getDescription());

    // Step 3: delete location
    locationRepository.deleteById(locationId);
  }

  @Test
  public void shouldReturnBadRequestForDefaultSiteModify() throws Exception {

    locationEntity.setIsDefault(YES);
    locationRepository.saveAndFlush(locationEntity);

    HttpHeaders headers = newCommonHeaders();
    mockMvc
        .perform(
            put(ApiEndpoint.UPDATE_LOCATION.getPath(), locationEntity.getId())
                .content(JsonUtils.asJsonString(getUpdateLocationRequest()))
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath(
                "$.error_description", is(ErrorCode.DEFAULT_SITE_MODIFY_DENIED.getDescription())));
  }

  @Test
  public void shouldReturnBadRequestForCannotReactivate() throws Exception {

    HttpHeaders headers = newCommonHeaders();
    UpdateLocationRequest updateLocationRequest = new UpdateLocationRequest();
    updateLocationRequest.setStatus(ACTIVE_STATUS);
    mockMvc
        .perform(
            put(ApiEndpoint.UPDATE_LOCATION.getPath(), locationEntity.getId())
                .content(JsonUtils.asJsonString(updateLocationRequest))
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error_description", is(ErrorCode.CANNOT_REACTIVE.getDescription())));
  }

  @Test
  public void shouldReturnBadRequestForCannotDecommissioned() throws Exception {

    locationEntity.setStatus(INACTIVE_STATUS);
    locationRepository.saveAndFlush(locationEntity);

    HttpHeaders headers = newCommonHeaders();
    UpdateLocationRequest updateLocationRequest = new UpdateLocationRequest();
    updateLocationRequest.setStatus(INACTIVE_STATUS);
    mockMvc
        .perform(
            put(ApiEndpoint.UPDATE_LOCATION.getPath(), locationEntity.getId())
                .content(JsonUtils.asJsonString(updateLocationRequest))
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.error_description", is(ErrorCode.ALREADY_DECOMMISSIONED.getDescription())));
  }

  @Test
  public void shouldReturnLocationNotFound() throws Exception {
    HttpHeaders headers = newCommonHeaders();
    mockMvc
        .perform(
            put(ApiEndpoint.UPDATE_LOCATION.getPath(), "3aed7a60-43c7-4bbd-94f9-f4a51de9f331")
                .content(JsonUtils.asJsonString(getUpdateLocationRequest()))
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.error_description", is(ErrorCode.LOCATION_NOT_FOUND.getDescription())));
  }

  @Test
  public void shouldUpdateALocation() throws Exception {
    HttpHeaders headers = newCommonHeaders();
    // Step 1: Call API to update location
    result =
        mockMvc
            .perform(
                put(ApiEndpoint.UPDATE_LOCATION.getPath(), locationEntity.getId())
                    .content(JsonUtils.asJsonString(getUpdateLocationRequest()))
                    .headers(headers)
                    .contextPath(getContextPath()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationId", notNullValue()))
            .andExpect(jsonPath("$.message", is(MessageCode.LOCATION_UPDATE_SUCCESS.getMessage())))
            .andReturn();

    String locationId = JsonPath.read(result.getResponse().getContentAsString(), "$.locationId");

    // Step 2: verify updated values
    Optional<LocationEntity> optLocationEntity = locationRepository.findById(locationId);
    LocationEntity locationEntity = optLocationEntity.get();
    assertNotNull(locationEntity);
    assertEquals(UPDATE_LOCATION_NAME_VALUE, locationEntity.getName());
    assertEquals(UPDATE_LOCATION_DESCRIPTION_VALUE, locationEntity.getDescription());

    // Step 3: delete location
    locationRepository.deleteById(locationId);
  }

  @Test
  public void shouldUpdateToReactiveLocation() throws Exception {

    locationEntity.setStatus(INACTIVE_STATUS);
    locationRepository.saveAndFlush(locationEntity);

    UpdateLocationRequest updateLocationRequest = new UpdateLocationRequest();
    updateLocationRequest.setStatus(ACTIVE_STATUS);

    HttpHeaders headers = newCommonHeaders();
    // Step 1: Call API to update location
    result =
        mockMvc
            .perform(
                put(ApiEndpoint.UPDATE_LOCATION.getPath(), locationEntity.getId())
                    .content(JsonUtils.asJsonString(updateLocationRequest))
                    .headers(headers)
                    .contextPath(getContextPath()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationId", notNullValue()))
            .andExpect(jsonPath("$.message", is(MessageCode.REACTIVE_SUCCESS.getMessage())))
            .andReturn();

    String locationId = JsonPath.read(result.getResponse().getContentAsString(), "$.locationId");

    // Step 2: verify updated values
    Optional<LocationEntity> optLocationEntity = locationRepository.findById(locationId);
    LocationEntity locationEntity = optLocationEntity.get();
    assertNotNull(locationEntity);
    assertEquals(ACTIVE_STATUS, locationEntity.getStatus().intValue());

    // Step 3: delete location
    locationRepository.deleteById(locationId);
  }

  @Test
  public void shouldReturnNotFoundForGetLocations() throws Exception {

    HttpHeaders headers = newCommonHeaders();

    mockMvc
        .perform(
            get(
                    ApiEndpoint.GET_LOCATION_WITH_LOCATION_ID.getPath(),
                    "b736fbf3-64e4-4f51-afe0-baab818d3f30")
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.error_description", is(ErrorCode.LOCATION_NOT_FOUND.getDescription())))
        .andReturn();
  }

  @Test
  public void shouldReturnLocations() throws Exception {

    HttpHeaders headers = newCommonHeaders();
    // with location Id
    mockMvc
        .perform(
            get(ApiEndpoint.GET_LOCATION_WITH_LOCATION_ID.getPath(), locationEntity.getId())
                .headers(headers)
                .contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.locations").isArray())
        .andExpect(jsonPath("$.locations[0].locationId", notNullValue()))
        .andExpect(jsonPath("$.locations", hasSize(1)))
        .andExpect(jsonPath("$.locations[0].studies").isArray());

    // TODO (Madhurya) need to check other values???
    // without location Id
    locationEntity = testDataHelper.createLocation();
    mockMvc
        .perform(
            get(ApiEndpoint.GET_LOCATIONS.getPath()).headers(headers).contextPath(getContextPath()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.locations").isArray())
        .andExpect(jsonPath("$.locations[0].locationId", notNullValue()))
        .andExpect(jsonPath("$.locations", hasSize(2)))
        .andExpect(jsonPath("$.locations[0].studies").isArray());
  }

  @AfterEach
  public void cleanUp() {
    testDataHelper.getUserRegAdminRepository().delete(userRegAdminEntity);
    testDataHelper.getLocationRepository().delete(locationEntity);
  }

  private LocationRequest getLocationRequest() throws JsonProcessingException {
    return new LocationRequest(CUSTOM_ID_VALUE, LOCATION_NAME_VALUE, LOCATION_DESCRIPTION_VALUE);
  }

  private UpdateLocationRequest getUpdateLocationRequest() throws JsonProcessingException {
    return new UpdateLocationRequest(UPDATE_LOCATION_NAME_VALUE, UPDATE_LOCATION_DESCRIPTION_VALUE);
  }

  public HttpHeaders newCommonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(CommonConstants.USER_ID_HEADER, userRegAdminEntity.getId());
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
