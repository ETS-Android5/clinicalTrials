# Copyright 2020 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

billing_account = "0103E1-E9BF45-92AA9E"
project_id      = "judealexis-studies-dev-devops"
state_bucket    = "judealexis-studies-dev-terraform-state"
terraform_root  = "deployment/terraform"
build_viewers = [
  "group:judealexis-studies-dev-cicd-viewers@clinicaltrials.one",
]
