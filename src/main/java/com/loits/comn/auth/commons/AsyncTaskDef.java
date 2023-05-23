package com.loits.comn.auth.commons;

public class AsyncTaskDef {
  public static final String TASK_COMPLETED = "completed";
  public static final String TASK_UPDATED = "in-progress";
  public static final String TASK_ERROR = "error";
  public static final String TASK_INITIATED = "initiated";

  public static class Module {
    public static final String RO = "Module-Auth";
  }

  public static class Task {
    public static final String NEW_ASYNC_OPERATION= "MAIN ASYNC OPERATION";
    public static final String NEW_UPDATE_PROVIDER = "UPDATE AUTH0";
    public static final String NEW_UPDATE_OTHER_TENANTS = "UPDATE OTHER TENANTS";

    //local vs auth data verification
    public static final String NEW_AUTH0_PERM_VERIFICATION = "AUTH0 PERMISSION VERIFICATION";
    public static final String AUTH0_PERM_LOCAL_UNAVAILABILITY = "AUTH0 PERMISSION NOT AVAILABLE LOCALLY";
    public static final String LOCAL_PERM_AUTH0_UNAVAILABILITY = "LOCAL PERMISSION NOT AVAILABLE IN AUTH0";

    public static final String NEW_AUTH0_ROLE_VERIFICATION = "AUTH0 ROLE VERIFICATION";
    public static final String AUTH0_ROLE_LOCAL_UNAVAILABILITY = "AUTH0 ROLE NOT AVAILABLE LOCALLY";
    public static final String LOCAL_ROLE_AUTH0_UNAVAILABILITY = "LOCAL ROLE NOT AVAILABLE IN AUTH0";
    public static final String AUTH0_ROLE_PERMISSION_LOCAL_UNAVAILABILITY = "AUTH0 ROLE PERMISSION MAPPING NOT AVAILABLE LOCALLY";
    public static final String LOCAL_ROLE_PERMISSION_AUTH0_UNAVAILABILITY = "LOCAL ROLE PERMISSION MAPPING NOT AVAILABLE IN AUTH0";


    public static final String NEW_AUTH0_GROUP_VERIFICATION = "AUTH0 GROUP VERIFICATION";
    public static final String AUTH0_GROUP_LOCAL_UNAVAILABILITY = "AUTH0 GROUP NOT AVAILABLE LOCALLY";
    public static final String LOCAL_GROUP_AUTH0_UNAVAILABILITY = "LOCAL GROUP NOT AVAILABLE IN AUTH0";
    public static final String AUTH0_GROUP_ROLE_LOCAL_UNAVAILABILITY = "AUTH0 GROUP ROLE MAPPING NOT AVAILABLE LOCALLY";
    public static final String LOCAL_GROUP_ROLE_AUTH0_UNAVAILABILITY = "LOCAL GROUP ROLE MAPPING NOT AVAILABLE IN AUTH0";

    public static final String NEW_AUTH0_USER_ROLE_VERIFICATION = "AUTH0 USER ROLE VERIFICATION";
    public static final String AUTH0_USER_ROLE_LOCAL_UNAVAILABILITY = "AUTH0 USER ROLE VERIFICATION";
    public static final String LOCAL_USER_ROLE_AUTH0_UNAVAILABILITY = "LOCAL USER ROLE MAPPING NOT AVAILABLE IN AUTH0";


    //permission creation
    public static final String NEW_PERMISSION_CREATION = "PERMISSION CREATION";
    public static final String NEW_AUTH0_PERMISSION_CREATION = "AUTH0 PERMISSION CREATION";
    public static final String NEW_OTHER_TENANT_PERMISSION_CREATION = "OTHER TENANT PERMISSION CREATION";

    //role creation
    public static final String NEW_ROLE_CREATION = "ROLE CREATION";
    public static final String NEW_AUTH0_ROLE_CREATION = "AUTH0 ROLE CREATION";
    public static final String NEW_OTHER_TENANT_ROLE_CREATION = "OTHER TENANT ROLE CREATION";

    //group creation
    public static final String NEW_ROLE_GROUP_CREATION = "ROLE-GROUP CREATION";
    public static final String NEW_AUTH0_GROUP_CREATION = "AUTH0 GROUP CREATION";
    public static final String NEW_OTHER_TENANT_ROLE_GROUP_CREATION = "OTHER TENANT ROLE-GROUP CREATION";

  }
}
