package com.mz.finalcommunity.finalcommunity.util;

public interface CommunityConstant {

    /**
     * Activated successfully
     */
    int ACTIVATION_SUCCESS = 0;

    /**
     * Repeated activation
     */
    int ACTIVATION_REPEAT = 1;

    /**
     * Activation fails
     */
    int ACTIVATION_FAILURE = 2;

    /**
     * Timeout for the default login credentials
     */
    int DEFAULT_EXPIRED_SECONDS = 3600 * 12;

    /**
     * Remember state login credentials timeout
     */
    int REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 100;

    /**
     * article
     */
    int ENTITY_TYPE_POST = 1;

    /**
     * comment
     */
    int ENTITY_TYPE_COMMENT = 2;

    /**
     * user
     */
    int ENTITY_TYPE_USER = 3;
}
