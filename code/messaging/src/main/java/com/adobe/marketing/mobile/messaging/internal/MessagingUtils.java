/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging.internal;

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.CACHE_BASE_DIR;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.ENDING_EVENT_ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.REQUEST_EVENT_ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.JSON_KEY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.IMAGES_CACHE_SUBDIRECTORY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.SchemaValues.SCHEMA_FEED_ITEM;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.SchemaValues.SCHEMA_IAM;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.SharedState.EdgeIdentity.ECID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.SharedState.EdgeIdentity.ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.SharedState.EdgeIdentity.IDENTITY_MAP;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.Proposition;
import com.adobe.marketing.mobile.Surface;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MessagingUtils {
    private final static String SELF_TAG = "MessagingUtils";

    static List<Proposition> getPropositionsFromPayloads(final List<Map<String, Object>> payloads) {
        final List<Proposition> propositions = new ArrayList<>();
        for (final Map<String, Object> payload : payloads) {
            if (payload != null) {
                final Proposition proposition = Proposition.fromEventData(payload);
                if (proposition != null) {
                    propositions.add(proposition);
                }
            }
        }
        return propositions;
    }

    // ========================================================================================
    // Cache Path helper
    // ========================================================================================

    static String getAssetCacheLocation() {
        final DeviceInforming deviceInfoService = ServiceProvider.getInstance().getDeviceInfoService();
        String assetCacheLocation = null;
        if (deviceInfoService != null) {
            final File applicationCacheDir = deviceInfoService.getApplicationCacheDir();
            if (applicationCacheDir != null) {
                assetCacheLocation = applicationCacheDir + File.separator + CACHE_BASE_DIR + File.separator + IMAGES_CACHE_SUBDIRECTORY;
            }
        }
        return assetCacheLocation;
    }

    // ========================================================================================
    // Event Validation
    // ========================================================================================

    /**
     * Determines if the passed in {@code Event} is a generic identity request content event.
     *
     * @param event A Generic Identity Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is a generic identity request content event.
     */
    static boolean isGenericIdentityRequestEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return EventType.GENERIC_IDENTITY.equalsIgnoreCase(event.getType()) &&
                EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource());
    }

    /**
     * Determines if the passed in {@code Event} is a messaging request content event.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is a messaging request content event.
     */
    static boolean isMessagingRequestContentEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return MessagingConstants.EventType.MESSAGING.equalsIgnoreCase(event.getType()) &&
                EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource());
    }

    /**
     * Determines if the passed in {@code Event} is a refresh messages event.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is a refresh messages event.
     */
    static boolean isRefreshMessagesEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return EventType.MESSAGING.equalsIgnoreCase(event.getType())
                && EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource())
                && event.getEventData().containsKey(MessagingConstants.EventDataKeys.Messaging.REFRESH_MESSAGES);
    }

    /**
     * Determines if the passed in {@code Event} is an edge personalization decision event.
     *
     * @param event An Edge Personalization Decision {@link Event}.
     * @return {@code boolean} indicating if the passed in event is an edge personalization decision event.
     */
    static boolean isEdgePersonalizationDecisionEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return MessagingConstants.EventType.EDGE.equalsIgnoreCase(event.getType()) &&
                MessagingConstants.EventSource.PERSONALIZATION_DECISIONS.equalsIgnoreCase(event.getSource());
    }

    /**
     * Determines if the passed in {@code Event} is an edge personalization complete event.
     *
     * @param event An Edge Personalization Complete {@link Event}.
     * @return {@code boolean} indicating if the passed in event is an edge personalization complete event.
     */
    static boolean isPersonalizationRequestCompleteEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return MessagingConstants.EventType.EDGE.equalsIgnoreCase(event.getType()) &&
                EventSource.CONTENT_COMPLETE.equalsIgnoreCase(event.getSource());
    }

    /**
     * Determines if the passed in {@code Event} is an update propositions event.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is an update propositions event.
     */
    static boolean isUpdatePropositionsEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return EventType.MESSAGING.equalsIgnoreCase(event.getType())
                && EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource())
                && DataReader.optBoolean(event.getEventData(), MessagingConstants.EventDataKeys.Messaging.UPDATE_PROPOSITIONS, false);
    }

    /**
     * Determines if the passed in {@code Event} is a get propositions event.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is a get propositions event.
     */
    static boolean isGetPropositionsEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return EventType.MESSAGING.equalsIgnoreCase(event.getType())
                && EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource())
                && DataReader.optBoolean(event.getEventData(), MessagingConstants.EventDataKeys.Messaging.GET_PROPOSITIONS, false);
    }

    // ========================================================================================
    // Surfaces retrieval and validation
    // ========================================================================================

    /**
     * Retrieves the app surfaces from the passed in {@code Event}'s event data.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code List<Surface>} containing the app surfaces to be used for retrieving propositions
     */
    static List<Surface> getSurfaces(final Event event) {
        if (event == null || event.getEventData() == null) {
            return null;
        }
        final List<String> surfaceUris = DataReader.optTypedList(String.class, event.getEventData(), MessagingConstants.EventDataKeys.Messaging.SURFACES, null);

        if (MessagingUtils.isNullOrEmpty(surfaceUris)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Surface URI's were not found in the provided event.");
            return null;
        }

        final List<Surface> surfaces = new ArrayList<>();
        for (final String surfaceUri : surfaceUris) {
            surfaces.add(Surface.fromUriString(surfaceUri));
        }
        return surfaces;
    }

    // ========================================================================================
    // Event id retrieval
    // ========================================================================================

    /**
     * Retrieves the request event id {@code String} from the passed in {@code Event}'s event data.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code String} containing the request event id
     */
    static String getRequestEventId(final Event event) {
        return DataReader.optString(event.getEventData(), REQUEST_EVENT_ID, null);
    }

    /**
     * Retrieves the ending event id {@code String} from the passed in {@code Event}'s event data.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code String} containing the ending event id
     */
    static String getEndingEventId(final Event event) {
        return DataReader.optString(event.getEventData(), ENDING_EVENT_ID, null);
    }

    // ========================================================================================
    // Consequence data retrieval from a JSONObject
    // ========================================================================================

    /**
     * Retrieves the consequence {@code JSONObject} from the passed in {@code JSONObject}.
     *
     * @param ruleJson A {@link JSONObject} containing an AJO rule payload
     * @return {@code JSONObject> containing the consequence extracted from the rule json
     */
    static JSONObject getConsequence(final JSONObject ruleJson) {
        JSONObject consequence = null;
        try {
            final JSONArray rulesArray = ruleJson.getJSONArray(JSON_KEY);
            final JSONArray consequenceArray = rulesArray.getJSONObject(0).getJSONArray(JSON_CONSEQUENCES_KEY);
            consequence = consequenceArray.getJSONObject(0);
        } catch (final JSONException jsonException) {
            Log.debug(LOG_TAG, "getConsequenceDetails", "Exception occurred retrieving rule consequence: %s", jsonException.getLocalizedMessage());
        }
        return consequence;
    }

    /**
     * Retrieves the consequence detail {@code Map} from the passed in {@code JSONObject}.
     *
     * @param ruleJson A {@link JSONObject} containing an AJO rule payload
     * @return {@code JSONObject> containing the consequence details extracted from the rule json
     */
    static JSONObject getConsequenceDetails(final JSONObject ruleJson) {
        JSONObject consequenceDetails = null;
        try {
            consequenceDetails = getConsequence(ruleJson).getJSONObject(MESSAGE_CONSEQUENCE_DETAIL);
        } catch (final JSONException jsonException) {
            Log.debug(LOG_TAG, "getConsequenceDetails", "Exception occurred retrieving consequence details: %s", jsonException.getLocalizedMessage());
        }
        return consequenceDetails;
    }

    // ========================================================================================
    // feed item type verification using rule consequence object
    // ========================================================================================
    static boolean isFeedItem(final RuleConsequence ruleConsequence) {
        final Map<String, Object> ruleDetailMap = ruleConsequence.getDetail();
        final String schema = DataReader.optString(ruleDetailMap, MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, "");
        return schema.equals(SCHEMA_FEED_ITEM);
    }

    // ========================================================================================
    // in app type verification using rule consequence object
    // ========================================================================================
    static boolean isInApp(final RuleConsequence ruleConsequence) {
        final Map<String, Object> ruleDetailMap = ruleConsequence.getDetail();
        final String schema = DataReader.optString(ruleDetailMap, MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, "");
        final String consequenceType = ruleConsequence.getType();
        return schema.equals(SCHEMA_IAM) || consequenceType.equals(MESSAGE_CONSEQUENCE_CJM_VALUE);
    }

    // ========================================================================================
    // Event Dispatching
    // ========================================================================================

    /**
     * Dispatches an event with the given parameters.
     *
     * @param eventName    a {@code String} containing the name of the event to be dispatched
     * @param eventType    a {@code String} containing the type of the event to be dispatched
     * @param eventSource  a {@code String} containing the source of the event to be dispatched
     * @param data         a {@link Map} containing the data of the event to be dispatched
     * @param mask         a {@link String[]} containing an optional event mask
     * @param extensionApi {@link ExtensionApi} to use for dispatching the event
     */
    static void sendEvent(final String eventName, final String eventType, final String eventSource, final Map<String, Object> data, final String[] mask, final ExtensionApi extensionApi) {
        final Event event = new Event.Builder(eventName, eventType, eventSource, mask)
                .setEventData(data)
                .build();
        extensionApi.dispatch(event);
    }

    /**
     * Dispatches an event with the given parameters.
     *
     * @param eventName    a {@code String} containing the name of the event to be dispatched
     * @param eventType    a {@code String} containing the type of the event to be dispatched
     * @param eventSource  a {@code String} containing the source of the event to be dispatched
     * @param data         a {@link Map} containing the data of the event to be dispatched
     * @param extensionApi {@link ExtensionApi} to use for dispatching the event
     */
    static void sendEvent(final String eventName, final String eventType, final String eventSource, final Map<String, Object> data, final ExtensionApi extensionApi) {
        sendEvent(eventName, eventType, eventSource, data, null, extensionApi);
    }

    // ========================================================================================
    // Shared State Helpers
    // ========================================================================================
    static String getSharedStateEcid(final Map<String, Object> edgeIdentityState) {
        final Map<String, Object> identityMap = DataReader.optTypedMap(Object.class, edgeIdentityState, IDENTITY_MAP, null);
        if (MapUtils.isNullOrEmpty(identityMap)) return null;

        final List<Map<String, Object>> ecids = DataReader.optTypedListOfMap(Object.class, identityMap, ECID, null);
        if (ecids == null || ecids.isEmpty()) return null;

        final Map<String, Object> ecidMap = ecids.get(0);
        if (MapUtils.isNullOrEmpty(ecidMap)) return null;

        return DataReader.optString(ecidMap, ID, null);
    }

    // ========================================================================================
    // Collection utils
    // ========================================================================================

    /**
     * Checks if the given {@code collection} is null or empty.
     *
     * @param collection input {@code Collection<?>} to be tested.
     * @return {@code boolean} result indicating whether the provided {@code collection} is null or empty.
     */
    static boolean isNullOrEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Returns a mutable {@code List<?>} list containing a single element.
     *
     * @param element A {@link Object} to be added to the mutable list
     * @return the mutable {@link List<?>} list
     */
    static List<?> createMutableList(final Object element) {
        return new ArrayList<Object>() {
            {
                add(element);
            }
        };
    }

    /**
     * Returns a mutable {@code List<?>} list containing a single element.
     *
     * @param list A {@link List<?>} to be converted to a mutable list
     * @return the mutable {@link List<?>} list
     */
    static List<?> createMutableList(final List<?> list) {
        return new ArrayList<>(list);
    }

    /**
     * Returns a mutable {@code List<Proposition>} list containing the contents of the passed in {@code List<Proposition>}.
     *
     * @param propositions A {@link List<Proposition>} to convert to a mutable list
     * @return the mutable {@link List<Proposition>} list
     */
    static List<Proposition> createMutablePropositionList(final List<Proposition> propositions) {
        return new ArrayList<>(propositions);
    }

    /**
     * Updates the provided {@code Map<Surface, List<Proposition>>} propositions map with the provided {@code Surface} and {@code Proposition} objects.
     *
     * @param surface         A {@link Surface} key used to update a {@link List<Proposition>} value in the provided {@link Map<Surface, List<Proposition>>}
     * @param proposition     A {@link Proposition} value to add in the provided {@code Map<Surface, List<Proposition>>}
     * @param propositionsMap The {@code Map<Surface, List<Proposition>>} to be updated with the provided {@code Surface} and {@code Proposition} objects
     * @return the updated {@link Map<Surface, List<Proposition>>} proposition maps
     */
    static Map<Surface, List<Proposition>> updatePropositionMapForSurface(final Surface surface, final Proposition proposition, final Map<Surface, List<Proposition>> propositionsMap) {
        final Map<Surface, List<Proposition>> tempPropositionsMap = new HashMap<>(propositionsMap);
        final List<Proposition> propositionList = tempPropositionsMap.get(surface) != null ? tempPropositionsMap.get(surface) : (List<Proposition>) createMutableList(proposition);
        if (tempPropositionsMap.get(surface) == null) {
            tempPropositionsMap.put(surface, propositionList);
        } else {
            tempPropositionsMap.get(surface).add(proposition);
        }
        return tempPropositionsMap;
    }

    /**
     * Updates the provided {@code Map<Surface, List<Proposition>>} propositions map with the provided {@code Surface} and {@code List<Proposition>} objects.
     *
     * @param surface         A {@link Surface} key used to update a {@link List<Proposition>} value in the provided {@link Map<Surface, List<Proposition>>}
     * @param newPropositions A {@link List<Proposition>} to add in the provided {@code Map<Surface, List<Proposition>>}
     * @param propositionsMap The {@link Map<Surface, List<Proposition>>} to be updated with the provided {@code Surface} and {@code Proposition} objects
     * @return the updated {@link Map<Surface, List<Proposition>>} proposition maps
     */
    static Map<Surface, List<Proposition>> updatePropositionMapForSurface(final Surface surface, final List<Proposition> newPropositions, final Map<Surface, List<Proposition>> propositionsMap) {
        if (MessagingUtils.isNullOrEmpty(newPropositions)) {
            return propositionsMap;
        }

        final Map<Surface, List<Proposition>> tempPropositionsMap = new HashMap<>(propositionsMap);
        final List<Proposition> propositionList = tempPropositionsMap.get(surface) != null ? tempPropositionsMap.get(surface) : (List<Proposition>) createMutableList(newPropositions);
        if (tempPropositionsMap.get(surface) == null) {
            tempPropositionsMap.put(surface, propositionList);
        } else {
            tempPropositionsMap.get(surface).addAll(newPropositions);
        }
        return tempPropositionsMap;
    }

    /**
     * Updates the provided {@code Map<Surface, List<LaunchRule>>} rule map with the provided {@code Surface} and {@code LaunchRule} objects.
     *
     * @param surface         A {@link Surface} key used to update a {@link List<LaunchRule>} value in the provided {@link Map<Surface, List<LaunchRule>>}
     * @param rules     A {@link List<LaunchRule>} list to add in the provided {@code Map<Surface, List<LaunchRule>>}
     * @param ruleMap The {@code Map<Surface, List<LaunchRule>>} to be updated with the provided {@code Surface} and {@code List<LaunchRule>} objects
     * @return the updated {@link Map<Surface, List<LaunchRule>>} rule maps
     */
    static Map<Surface, List<LaunchRule>> updateRuleMapForSurface(final Surface surface, final List<LaunchRule> rules, final Map<Surface, List<LaunchRule>> ruleMap) {
        final Map<Surface, List<LaunchRule>> tempRulesMap = new HashMap<>(ruleMap);
        final List<LaunchRule> ruleList = tempRulesMap.get(surface) != null ? tempRulesMap.get(surface) : (List<LaunchRule>) MessagingUtils.createMutableList(rules);
        if (tempRulesMap.get(surface) == null) {
            tempRulesMap.put(surface, ruleList);
        } else {
            tempRulesMap.get(surface).addAll(ruleList);
        }
        return tempRulesMap;
    }

}