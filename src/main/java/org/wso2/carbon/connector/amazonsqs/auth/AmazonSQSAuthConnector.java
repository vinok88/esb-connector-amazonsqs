/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.connector.amazonsqs.auth;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.wso2.carbon.connector.amazonsqs.constants.AmazonSQSConstants;
import org.wso2.carbon.connector.core.AbstractConnector;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class AmazonSQSAuthConnector which helps to generate authentication signature for Amazon SQS WSO2 ESB
 * Connector.
 */
public class AmazonSQSAuthConnector extends AbstractConnector {
    /**
     * Connect method which is generating authentication of the connector for each request.
     *
     * @param messageContext ESB messageContext.
     */
    public final void connect(final MessageContext messageContext) {
        final StringBuilder canonicalRequest = new StringBuilder();
        final StringBuilder stringToSign = new StringBuilder();
      //  final StringBuilder payloadBuilder = new StringBuilder();
      //  final StringBuilder payloadStrBuilder = new StringBuilder();
        final StringBuilder authHeader = new StringBuilder();

        /**
         * <property name="uri.var.signatureMethod" value="HmacSHA256"/>
         <property name="uri.var.signatureVersion" value="4"/>
         <property name="uri.var.contentType" expression="$func:contentType"/>
         <property name="uri.var.httpMethod" value="POST"/>
         <property name="uri.var.terminationString" value="aws4_request"/>
         <property name="POST_TO_URI" value="true" scope="axis2"/>
         <property name="DISABLE_CHUNKING" value="true" scope="axis2"/>
         <header name="Content-Type" scope="transport" expression="get-property('uri.var.contentType')" />
         <property name="messageType" expression="get-property('uri.var.contentType')" scope="axis2"/>
         * **/
        //custom properties added
        messageContext.setProperty("uri.var.terminationString", "aws4_request");
        messageContext.setProperty("uri.var.signatureMethod", "HmacSHA256");
        messageContext.setProperty("uri.var.contentType", "application/json");
        messageContext.setProperty("uri.var.httpMethod", "POST");
        messageContext.setProperty("uri.var.host.id", "961134029637");
        messageContext.setProperty("uri.var.host.table", "table");
        messageContext.setProperty("uri.var.host.name", "dev-foi");

        // Generate time-stamp which will be sent to API and to be used in Signature
        final Date date = new Date();
        // API Recommends to use GMT Time zone
        final TimeZone timeZone = TimeZone.getTimeZone(AmazonSQSConstants.GMT);
        final DateFormat dateFormat = new SimpleDateFormat(AmazonSQSConstants.ISO8601_BASIC_DATE_FORMAT);
        dateFormat.setTimeZone(timeZone);
        final String amzDate = dateFormat.format(date);

        final DateFormat shortDateFormat = new SimpleDateFormat(AmazonSQSConstants.SHORT_DATE_FORMAT);
        shortDateFormat.setTimeZone(timeZone);
        final String shortDate = shortDateFormat.format(date);

        messageContext.setProperty(AmazonSQSConstants.AMZ_DATE, amzDate);
        final Map<String, String> parameterNamesMap = getParameterNamesMap();
        //final Map<String, String> parametersMap = getSortedParametersMap(messageContext, parameterNamesMap);
        try {
            canonicalRequest.append(messageContext.getProperty(AmazonSQSConstants.HTTP_METHOD));
            canonicalRequest.append(AmazonSQSConstants.NEW_LINE);
            final String charSet = Charset.defaultCharset().toString();
//            if (messageContext.getProperty(AmazonSQSConstants.URL_QUEUE_NAME) != null
//                    && !("").equals(messageContext.getProperty(AmazonSQSConstants.URL_QUEUE_NAME))
//                    && messageContext.getProperty(AmazonSQSConstants.QUEUE_ID) != null
//                    && !("").equals(messageContext.getProperty(AmazonSQSConstants.QUEUE_ID))) {
//                // queue ID and queue name should be encoded twise to match the Signature being generated by
//                // API,
//                // Note that API it looks encodes the incoming URL once before creating the signature, SInce
//                // we send url encoded URLs, API signatures are twise encoded
//                final String encodedQueueID =
//                        URLEncoder.encode(messageContext.getProperty(AmazonSQSConstants.QUEUE_ID).toString(), charSet);
//                final String encodedQueueName =
//                        URLEncoder.encode(messageContext.getProperty(AmazonSQSConstants.URL_QUEUE_NAME).toString(),
//                                charSet);
//                canonicalRequest
//                        .append((AmazonSQSConstants.FORWARD_SLASH + URLEncoder.encode(encodedQueueID, charSet)
//                                + AmazonSQSConstants.FORWARD_SLASH + URLEncoder.encode(encodedQueueName, charSet) + AmazonSQSConstants.FORWARD_SLASH)
//                                .replaceAll(AmazonSQSConstants.REGEX_ASTERISK, AmazonSQSConstants.URL_ENCODED_ASTERISK));
//                // Sets the http request Uri to message context
//                messageContext.setProperty(AmazonSQSConstants.HTTP_REQUEST_URI, AmazonSQSConstants.FORWARD_SLASH
//                        + encodedQueueID + AmazonSQSConstants.FORWARD_SLASH + encodedQueueName
//                        + AmazonSQSConstants.FORWARD_SLASH);
//            } else {
//                canonicalRequest.append(AmazonSQSConstants.FORWARD_SLASH);
//            }
            //append absolute URI
            final String tableId = URLEncoder.encode(messageContext.getProperty("uri.var.host.id").toString(), charSet);
            final String table = URLEncoder.encode(messageContext.getProperty("uri.var.host.table").toString(),
                    charSet);
            final String tableName = URLEncoder.encode(messageContext.getProperty("uri.var.host.name").toString(),
                    charSet);
            final String encodedAbsoluteUrl = AmazonSQSConstants.FORWARD_SLASH + tableId + AmazonSQSConstants
                    .FORWARD_SLASH + table + AmazonSQSConstants.FORWARD_SLASH + tableName + AmazonSQSConstants
                    .FORWARD_SLASH;
            canonicalRequest.append(encodedAbsoluteUrl);
            canonicalRequest.append(AmazonSQSConstants.NEW_LINE);

//             Creates the payload Builder
//            for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
//                payloadBuilder.append(URLEncoder.encode(entry.getKey(), charSet));
//                payloadBuilder.append(AmazonSQSConstants.EQUAL);
//                if(entry.getKey().equals(AmazonSQSConstants.API_MESSAGE_BODY)
//                        && AmazonSQSConstants.JSON_START_CHARACTER.contains(entry.getValue().substring(0, 1))) {
//                    payloadBuilder.append(URLEncoder.encode(entry.getValue().replace("\\\"", "\""), charSet));
//                } else {
//                    payloadBuilder.append(URLEncoder.encode(entry.getValue(), charSet));
//                }
//                payloadBuilder.append(AmazonSQSConstants.AMPERSAND);
//                payloadStrBuilder.append('"');
//                payloadStrBuilder.append(entry.getKey());
//                payloadStrBuilder.append('"');
//                payloadStrBuilder.append(AmazonSQSConstants.COLON);
//                payloadStrBuilder.append('"');
//                if(entry.getKey().equals(AmazonSQSConstants.API_MESSAGE_BODY)
//                        && AmazonSQSConstants.JSON_START_CHARACTER.contains(entry.getValue().substring(0, 1))) {
//                    payloadStrBuilder.append(entry.getValue().replace("\"", "\\\"").replace("\\\\\"", "\\\""));
//                } else {
//                    payloadStrBuilder.append(entry.getValue());
//                }
//                payloadStrBuilder.append('"');
//                payloadStrBuilder.append(AmazonSQSConstants.COMMA);
//            }

//            if (payloadStrBuilder.length() > 0) {
//                messageContext.setProperty(AmazonSQSConstants.REQUEST_PAYLOAD,
//                        "{" + payloadStrBuilder.substring(0, payloadStrBuilder.length() - 1) + "}");
//            }
            // Appends empty string since no URL parameters are used in POST API requests
            canonicalRequest.append(AmazonSQSConstants.SPACE);
            canonicalRequest.append(AmazonSQSConstants.NEW_LINE);

            final Map<String, String> headersMap = getSortedHeadersMap(messageContext, parameterNamesMap);
            final StringBuilder canonicalHeaders = new StringBuilder();
            final StringBuilder signedHeader = new StringBuilder();
            // Builds canonical headers.
            for (Map.Entry<String, String> entry : headersMap.entrySet()) {
                canonicalHeaders.append(entry.getKey());
                canonicalHeaders.append(AmazonSQSConstants.COLON);
                canonicalHeaders.append(entry.getValue());
                canonicalHeaders.append(AmazonSQSConstants.NEW_LINE);
                signedHeader.append(entry.getKey());
                signedHeader.append(AmazonSQSConstants.SEMI_COLON);
            }

            canonicalRequest.append(canonicalHeaders.toString());
            canonicalRequest.append(AmazonSQSConstants.NEW_LINE);
            // Remove unwanted semi-colon at the end of the signedHeader string
            String signedHeaders = "";
            if (signedHeader.length() > 0) {
                signedHeaders = signedHeader.substring(0, signedHeader.length() - 1);
            }
            canonicalRequest.append(signedHeaders);
            canonicalRequest.append(AmazonSQSConstants.NEW_LINE);
            // HashedPayload = HexEncode(Hash(requestPayload))
            String requestPayload = "";
//            if (payloadBuilder.length() > 0) {
                /*
                 * First removes the additional ampersand appended to the end of the payloadBuilder, then o
                 * further modifications to preserve unreserved characters as per the API guide
                 * (http://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html)
                 */
//                requestPayload =
//                        payloadBuilder.substring(0, payloadBuilder.length() - 1).toString()
//                                .replace(AmazonSQSConstants.PLUS, AmazonSQSConstants.URL_ENCODED_PLUS)
//                                .replace(AmazonSQSConstants.URL_ENCODED_TILT, AmazonSQSConstants.TILT);
                requestPayload = (messageContext.getProperty(AmazonSQSConstants.REQUEST_PAYLOAD)).toString();
//            }
            canonicalRequest.append(bytesToHex(hash(messageContext, requestPayload)).toLowerCase());
            System.out.println("Canonical String:\n"+ canonicalRequest.toString());

            stringToSign.append(AmazonSQSConstants.AWS4_HMAC_SHA_256);
            stringToSign.append(AmazonSQSConstants.NEW_LINE);
            stringToSign.append(amzDate);
            stringToSign.append(AmazonSQSConstants.NEW_LINE);
            stringToSign.append(shortDate);
            stringToSign.append(AmazonSQSConstants.FORWARD_SLASH);
            stringToSign.append(messageContext.getProperty(AmazonSQSConstants.REGION));
            stringToSign.append(AmazonSQSConstants.FORWARD_SLASH);
            stringToSign.append(messageContext.getProperty(AmazonSQSConstants.SERVICE));
            stringToSign.append(AmazonSQSConstants.FORWARD_SLASH);
            stringToSign.append(messageContext.getProperty(AmazonSQSConstants.TERMINATION_STRING));
            stringToSign.append(AmazonSQSConstants.NEW_LINE);
            stringToSign.append(base16encode(hash(messageContext, canonicalRequest.toString())).toLowerCase());
            final byte[] signingKey =
                    getSignatureKey(messageContext, messageContext.getProperty(AmazonSQSConstants.SECRET_ACCESS_KEY)
                            .toString(), shortDate, messageContext.getProperty(AmazonSQSConstants.REGION).toString(),
                            messageContext.getProperty(AmazonSQSConstants.SERVICE).toString());

            System.out.println("String to sign:\n" + stringToSign.toString());

            // Construction of authorization header value to be included in API request
            authHeader.append(AmazonSQSConstants.AWS4_HMAC_SHA_256);
            authHeader.append(AmazonSQSConstants.SPACE);
            authHeader.append(AmazonSQSConstants.CREDENTIAL);
            authHeader.append(AmazonSQSConstants.EQUAL);
            authHeader.append(messageContext.getProperty(AmazonSQSConstants.ACCESS_KEY_ID));
            authHeader.append(AmazonSQSConstants.FORWARD_SLASH);
            authHeader.append(shortDate);
            authHeader.append(AmazonSQSConstants.FORWARD_SLASH);
            authHeader.append(messageContext.getProperty(AmazonSQSConstants.REGION));
            authHeader.append(AmazonSQSConstants.FORWARD_SLASH);
            authHeader.append(messageContext.getProperty(AmazonSQSConstants.SERVICE));
            authHeader.append(AmazonSQSConstants.FORWARD_SLASH);
            authHeader.append(messageContext.getProperty(AmazonSQSConstants.TERMINATION_STRING));
            authHeader.append(AmazonSQSConstants.COMMA);
            authHeader.append(AmazonSQSConstants.SPACE);
            authHeader.append(AmazonSQSConstants.SIGNED_HEADERS);
            authHeader.append(AmazonSQSConstants.EQUAL);
            authHeader.append(signedHeaders);
            authHeader.append(AmazonSQSConstants.COMMA);
            authHeader.append(AmazonSQSConstants.SPACE);
            authHeader.append(AmazonSQSConstants.API_SIGNATURE);
            authHeader.append(AmazonSQSConstants.EQUAL);
            authHeader.append(bytesToHex(hmacSHA256(signingKey, stringToSign.toString())).toLowerCase());
            // Adds authorization header to message context
            messageContext.setProperty(AmazonSQSConstants.AUTHORIZATION_HEADER, authHeader.toString());
        } catch (InvalidKeyException exc) {
            log.error(AmazonSQSConstants.INVALID_KEY_ERROR, exc);
            storeErrorResponseStatus(messageContext, exc, AmazonSQSConstants.INVALID_KEY_ERROR_CODE);
            handleException(AmazonSQSConstants.INVALID_KEY_ERROR, exc, messageContext);
        } catch (NoSuchAlgorithmException exc) {
            log.error(AmazonSQSConstants.NO_SUCH_ALGORITHM_ERROR, exc);
            storeErrorResponseStatus(messageContext, exc, AmazonSQSConstants.NO_SUCH_ALGORITHM_ERROR_CODE);
            handleException(AmazonSQSConstants.NO_SUCH_ALGORITHM_ERROR, exc, messageContext);
        } catch (IllegalStateException exc) {
            log.error(AmazonSQSConstants.ILLEGAL_STATE_ERROR, exc);
            storeErrorResponseStatus(messageContext, exc, AmazonSQSConstants.ILLEGAL_STATE_ERROR_CODE);
            handleException(AmazonSQSConstants.CONNECTOR_ERROR, exc, messageContext);
        } catch (UnsupportedEncodingException exc) {
            log.error(AmazonSQSConstants.UNSUPPORTED_ENCORDING_ERROR, exc);
            storeErrorResponseStatus(messageContext, exc, AmazonSQSConstants.UNSUPPORTED_ENCORDING_ERROR_CODE);
            handleException(AmazonSQSConstants.CONNECTOR_ERROR, exc, messageContext);
        }

    }

    /**
     * getKeys method returns a list of parameter keys.
     *
     * @return list of parameter key value.
     */
    private String[] getParameterKeys() {

        return new String[] { AmazonSQSConstants.ACTION, AmazonSQSConstants.EXPIRES, AmazonSQSConstants.SECURITY_TOKEN,
                AmazonSQSConstants.SIGNATURE, AmazonSQSConstants.SIGNATURE_METHOD,
                AmazonSQSConstants.SIGNATURE_VERSION, AmazonSQSConstants.TIMESTAMP, AmazonSQSConstants.VERSION,
                AmazonSQSConstants.QUEUE_NAME_PREFIX, AmazonSQSConstants.QUEUE_URLS, AmazonSQSConstants.ACCESS_KEY_ID,
                AmazonSQSConstants.PAYLOAD_QUEUE_NAME, AmazonSQSConstants.LABEL, AmazonSQSConstants.MESSAGE_BODY,
                AmazonSQSConstants.LABEL, AmazonSQSConstants.MESSAGE_BODY, AmazonSQSConstants.RECEIPT_HANDLE,
                AmazonSQSConstants.MAX_NO_OF_MESSAGES, AmazonSQSConstants.VISIBILITY_TIMEOUT,
                AmazonSQSConstants.WAIT_TIME_SECONDS, AmazonSQSConstants.DELAY_SECONDS, AmazonSQSConstants.AWS_ACCOUNT_ID

        };
    }

    /**
     * getKeys method returns a list of header keys.
     *
     * @return list of header key value.
     */
    private String[] getHeaderKeys() {
    
        return new String[] { AmazonSQSConstants.CONTENT_TYPE, AmazonSQSConstants.HOST, AmazonSQSConstants.AMZ_DATE,
                              AmazonSQSConstants.AMZ_TARGET};
    }

    /**
     * getCollectionParameterKeys method returns a list of predefined parameter keys which users will be used.
     * to send collection of values in each parameter.
     *
     * @return list of parameter key value.
     */
    private String[] getMultivaluedParameterKeys() {

        return new String[] { AmazonSQSConstants.AWS_ACCOUNT_NUMBERS, AmazonSQSConstants.ACTION_NAMES,
                AmazonSQSConstants.REQUEST_ENTRIES, AmazonSQSConstants.ATTRIBUTE_ENTRIES,
                AmazonSQSConstants.ATTRIBUTES, AmazonSQSConstants.MESSAGE_ATTRIBUTE_NAMES,
                AmazonSQSConstants.MESSAGE_ATTRIBUTES, AmazonSQSConstants.MESSAGE_REQUEST_ENTRY };
    }

    /**
     * getParametersMap method used to return list of parameter values sorted by expected API parameter names.
     *
     * @param messageContext ESB messageContext.
     * @param namesMap contains a map of esb parameter names and matching API parameter names
     * @return assigned parameter values as a HashMap.
     */
    private Map<String, String> getSortedParametersMap(final MessageContext messageContext,
            final Map<String, String> namesMap) {

        final String[] singleValuedKeys = getParameterKeys();
        final Map<String, String> parametersMap = new TreeMap<String, String>();
        // Stores sorted, single valued API parameters
        for (byte index = 0; index < singleValuedKeys.length; index++) {
            final String key = singleValuedKeys[index];
            // builds the parameter map only if provided by the user
            if (messageContext.getProperty(key) != null && !("").equals((String) messageContext.getProperty(key))) {
                parametersMap.put(namesMap.get(key), (String) messageContext.getProperty(key));
            }
        }
        final String[] multiValuedKeys = getMultivaluedParameterKeys();
        // Stores sorted, multi-valued API parameters
        for (byte index = 0; index < multiValuedKeys.length; index++) {
            final String key = multiValuedKeys[index];
            // builds the parameter map only if provided by the user
            if (messageContext.getProperty(key) != null && !("").equals((String) messageContext.getProperty(key))) {
                final String collectionParam = (String) messageContext.getProperty(key);
                // Splits the collection parameter to retrieve parameters separately
                final String[] keyValuepairs = collectionParam.split(AmazonSQSConstants.AMPERSAND);
                for (String keyValue : keyValuepairs) {
                    if (keyValue.contains(AmazonSQSConstants.EQUAL)
                            && keyValue.split(AmazonSQSConstants.EQUAL).length == AmazonSQSConstants.TWO) {
                        // Split the key and value of parameters to be sent to API
                        parametersMap.put(keyValue.split(AmazonSQSConstants.EQUAL)[0],
                                keyValue.split(AmazonSQSConstants.EQUAL)[1]);
                    } else {
                        log.error(AmazonSQSConstants.INVALID_PARAMETERS + keyValue);
                        storeErrorResponseStatus(messageContext, AmazonSQSConstants.INVALID_PARAMETERS,
                                AmazonSQSConstants.ILLEGAL_ARGUMENT_ERROR_CODE);
                        handleException(AmazonSQSConstants.INVALID_PARAMETERS, new IllegalArgumentException(), messageContext);
                    }
                }
            }
        }
        return parametersMap;
    }

    /**
     * getSortedHeadersMap method used to return list of header values sorted by expected API parameter names.
     *
     * @param messageContext ESB messageContext.
     * @param namesMap contains a map of esb parameter names and matching API parameter names
     * @return assigned header values as a HashMap.
     */
    private Map<String, String> getSortedHeadersMap(final MessageContext messageContext,
            final Map<String, String> namesMap) {

        final String[] headerKeys = getHeaderKeys();
        final Map<String, String> parametersMap = new TreeMap<String, String>();
        // Stores sorted, single valued API parameters
        for (byte index = 0; index < headerKeys.length; index++) {
            final String key = headerKeys[index];
            // builds the parameter map only if provided by the user
            if (messageContext.getProperty(key) != null && !("").equals((String) messageContext.getProperty(key))) {
                parametersMap.put(namesMap.get(key).toLowerCase(), messageContext.getProperty(key).toString().trim()
                        .replaceAll(AmazonSQSConstants.TRIM_SPACE_REGEX, AmazonSQSConstants.SPACE));
            }
        }
        return parametersMap;
    }

    /**
     * getparameterNamesMap returns a map of esb parameter names and corresponding API parameter names.
     *
     * @return generated map.
     */
    private Map<String, String> getParameterNamesMap() {

        final Map<String, String> map = new HashMap<String, String>();
//        map.put(AmazonSQSConstants.ACTION, AmazonSQSConstants.API_ACTION);
//        map.put(AmazonSQSConstants.EXPIRES, AmazonSQSConstants.API_EXPIRES);
//        map.put(AmazonSQSConstants.SECURITY_TOKEN, AmazonSQSConstants.API_SECURITY_TOKEN);
//        map.put(AmazonSQSConstants.SIGNATURE, AmazonSQSConstants.API_SIGNATURE);
        map.put(AmazonSQSConstants.SIGNATURE_METHOD, AmazonSQSConstants.API_SIGNATURE_METHOD);
//        map.put(AmazonSQSConstants.SIGNATURE_VERSION, AmazonSQSConstants.API_SIGNATURE_VERSION);
//        map.put(AmazonSQSConstants.TIMESTAMP, AmazonSQSConstants.API_TIMESTAMP);
        map.put(AmazonSQSConstants.VERSION, AmazonSQSConstants.API_VERSION);
  //      map.put(AmazonSQSConstants.ACCESS_KEY_ID, AmazonSQSConstants.AWS_ACCESS_KEY_ID);
//        map.put(AmazonSQSConstants.QUEUE_NAME_PREFIX, AmazonSQSConstants.API_QUEUE_NAME_PREFIX);
//        map.put(AmazonSQSConstants.QUEUE_URLS, AmazonSQSConstants.API_QUEUE_URLS);
//        map.put(AmazonSQSConstants.LABEL, AmazonSQSConstants.API_LABEL);
//        map.put(AmazonSQSConstants.PAYLOAD_QUEUE_NAME, AmazonSQSConstants.API_QUEUE_NAME);
//        map.put(AmazonSQSConstants.MESSAGE_BODY, AmazonSQSConstants.API_MESSAGE_BODY);
//        map.put(AmazonSQSConstants.RECEIPT_HANDLE, AmazonSQSConstants.API_RECEIPT_HANDLE);
//        map.put(AmazonSQSConstants.MAX_NO_OF_MESSAGES, AmazonSQSConstants.API_MAX_NO_OF_MESSAGES);
//        map.put(AmazonSQSConstants.VISIBILITY_TIMEOUT, AmazonSQSConstants.API_VISIBILITY_TIMEOUT);
//        map.put(AmazonSQSConstants.WAIT_TIME_SECONDS, AmazonSQSConstants.API_WAIT_TIME_SECONDS);
//        map.put(AmazonSQSConstants.DELAY_SECONDS, AmazonSQSConstants.API_DELAY_SECONDS);
//        map.put(AmazonSQSConstants.AWS_ACCOUNT_ID, AmazonSQSConstants.API_ACCOUNT_ID);
        // Header parameters
        map.put(AmazonSQSConstants.HOST, AmazonSQSConstants.API_HOST);
        map.put(AmazonSQSConstants.CONTENT_TYPE, AmazonSQSConstants.API_CONTENT_TYPE);
        map.put(AmazonSQSConstants.AMZ_DATE, AmazonSQSConstants.API_AMZ_DATE);
        map.put(AmazonSQSConstants.AMZ_TARGET, AmazonSQSConstants.REQUEST_TARGET);

        return map;
    }

    /**
     * Add a Throwable to a message context, the message from the throwable is embedded as the Synapse.
     * Constant ERROR_MESSAGE.
     *
     * @param ctxt message context to which the error tags need to be added
     * @param throwable Throwable that needs to be parsed and added
     * @param errorCode errorCode mapped to the exception
     */
    public final void storeErrorResponseStatus(final MessageContext ctxt, final Throwable throwable, final int errorCode) {

        ctxt.setProperty(SynapseConstants.ERROR_CODE, errorCode);
        ctxt.setProperty(SynapseConstants.ERROR_MESSAGE, throwable.getMessage());
        ctxt.setFaultResponse(true);
    }

    /**
     * Add a message to message context, the message from the throwable is embedded as the Synapse Constant
     * ERROR_MESSAGE.
     *
     * @param ctxt message context to which the error tags need to be added
     * @param message message to be returned to the user
     * @param errorCode errorCode mapped to the exception
     */
    public final void storeErrorResponseStatus(final MessageContext ctxt, final String message, final int errorCode) {
        ctxt.setProperty(SynapseConstants.ERROR_CODE, errorCode);
        ctxt.setProperty(SynapseConstants.ERROR_MESSAGE, message);
        ctxt.setFaultResponse(true);
    }

    /**
     * Hashes the string contents (assumed to be UTF-8) using the SHA-256 algorithm.
     *
     * @param messageContext of the connector
     * @param text text to be hashed
     * @return SHA-256 hashed text
     */
    public final byte[] hash(final MessageContext messageContext, final String text) {

        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance(AmazonSQSConstants.SHA_256);
            messageDigest.update(text.getBytes(AmazonSQSConstants.UTF_8));
        } catch (Exception exc) {
            log.error(AmazonSQSConstants.CONNECTOR_ERROR, exc);
            storeErrorResponseStatus(messageContext, exc, AmazonSQSConstants.ERROR_CODE_EXCEPTION);
            handleException(AmazonSQSConstants.CONNECTOR_ERROR, exc, messageContext);
        }
        if (messageDigest == null) {
            log.error(AmazonSQSConstants.CONNECTOR_ERROR);
            storeErrorResponseStatus(messageContext, AmazonSQSConstants.CONNECTOR_ERROR,
                    AmazonSQSConstants.ERROR_CODE_EXCEPTION);
            handleException(AmazonSQSConstants.CONNECTOR_ERROR, messageContext);
        }
        return messageDigest.digest();
    }

    /**
     * bytesToHex method HexEncoded the received byte array.
     *
     * @param bytes bytes to be hex encoded
     * @return hex encoded String of the given byte array
     */
    public static String bytesToHex(final byte[] bytes) {

        final char[] hexArray = AmazonSQSConstants.HEX_ARRAY_STRING.toCharArray();
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            final int byteVal = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[byteVal >>> 4];
            hexChars[j * 2 + 1] = hexArray[byteVal & 0x0F];
        }
        return new String(hexChars);
    }

    /** my bytes to hex*/
    private final static char[] HEX = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * Convert bytes to a base16 string.
     */
    public static String base16encode(byte[] byteArray) {
        StringBuffer hexBuffer = new StringBuffer(byteArray.length * 2);
        for (int i = 0; i < byteArray.length; i++)
            for (int j = 1; j >= 0; j--)
                hexBuffer.append(HEX[(byteArray[i] >> (j * 4)) & 0xF]);
        return hexBuffer.toString();
    }

    /**
     * Returns the encoded signature key to be used for further encodings as per API doc.
     *
     * @param ctx message context of the connector
     * @param key key to be used for signing
     * @param dateStamp current date stamp
     * @param regionName region name given to the connector
     * @param serviceName Name of the service being addressed
     * @return Signature key
     * @throws java.io.UnsupportedEncodingException Unsupported Encoding Exception
     * @throws IllegalStateException Illegal Argument Exception
     * @throws java.security.NoSuchAlgorithmException No Such Algorithm Exception
     * @throws java.security.InvalidKeyException Invalid Key Exception
     */
    private byte[] getSignatureKey(final MessageContext ctx, final String key, final String dateStamp,
            final String regionName, final String serviceName) throws UnsupportedEncodingException,
            InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {

        final byte[] kSecret = (AmazonSQSConstants.AWS4 + key).getBytes(AmazonSQSConstants.UTF8);
        final byte[] kDate = hmacSHA256(kSecret, dateStamp);
        final byte[] kRegion = hmacSHA256(kDate, regionName);
        final byte[] kService = hmacSHA256(kRegion, serviceName);
        return hmacSHA256(kService, ctx.getProperty(AmazonSQSConstants.TERMINATION_STRING).toString());
    }

    /**
     * Provides the HMAC SHA 256 encoded value(using the provided key) of the given data.
     *
     * @param key to use for encoding
     * @param data to be encoded
     * @return HMAC SHA 256 encoded byte array
     * @throws java.security.NoSuchAlgorithmException No such algorithm Exception
     * @throws java.security.InvalidKeyException Invalid key Exception
     * @throws java.io.UnsupportedEncodingException Unsupported Encoding Exception
     * @throws IllegalStateException Illegal State Exception
     */
    private byte[] hmacSHA256(final byte[] key, final String data) throws NoSuchAlgorithmException,
            InvalidKeyException, IllegalStateException, UnsupportedEncodingException {

        final String algorithm = AmazonSQSConstants.HAMC_SHA_256;
        final Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data.getBytes(AmazonSQSConstants.UTF8));
    }
}
