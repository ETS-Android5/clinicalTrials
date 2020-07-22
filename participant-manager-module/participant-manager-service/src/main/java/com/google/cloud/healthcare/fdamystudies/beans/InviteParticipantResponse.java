/*
 * Copyright 2020 Google LLC
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package com.google.cloud.healthcare.fdamystudies.beans;

import java.util.LinkedList;
import java.util.List;

import com.google.cloud.healthcare.fdamystudies.common.ErrorCode;
import com.google.cloud.healthcare.fdamystudies.common.MessageCode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InviteParticipantResponse extends BaseResponse {

  private List<String> ids = new LinkedList<>();

  private List<String> successIds = new LinkedList<>();

  private List<String> failedInvitations = new LinkedList<>();

  public InviteParticipantResponse(ErrorCode errorCode) {
    super(errorCode);
  }

  public InviteParticipantResponse(MessageCode messageCode) {
    super(messageCode);
  }
}
