package hudson.plugins.blazemeter.api;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.mail.MessagingException;
import javax.servlet.ServletException;

import hudson.plugins.blazemeter.api.urlmanager.BmUrlManager;
import hudson.plugins.blazemeter.api.urlmanager.BmUrlManagerV2Impl;
import hudson.plugins.blazemeter.api.urlmanager.URLFactory;
import hudson.plugins.blazemeter.entities.TestInfo;
import hudson.plugins.blazemeter.entities.TestStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * User: Vitali
 * Date: 4/2/12
 * Time: 14:05
 * <p/>
 * Updated
 * User: Doron
 * Date: 8/7/12
 *
 * Updated (proxy)
 * User: Marcel
 * Date: 9/23/13

 */

public class BlazemeterApiV2Impl implements BlazemeterApi{
    PrintStream logger = new PrintStream(System.out);

    BmUrlManager urlManager;
    private BZMHTTPClient bzmhc = null;
    BlazemeterApiV2Impl() {
        urlManager = URLFactory.getURLFactory().
                getURLManager(URLFactory.ApiVersion.v2,"https://a.blazemeter.com");
        try {
            bzmhc = BZMHTTPClient.getInstance();
            bzmhc.configureProxy();
        } catch (Exception ex) {
            logger.format("error Instantiating HTTPClient. Exception received: %s", ex);
        }
    }


    /**
     * @param userKey  - user key
     * @param testId   - test id
     * @param file     - jmx file
     *                 //     * @return test id
     *                 //     * @throws java.io.IOException
     *                 //     * @throws org.json.JSONException
     */
    public synchronized void uploadJmx(String userKey, String testId, File file) {

        if (!validate(userKey, testId)) return;

        String url = this.urlManager.scriptUpload(APP_KEY, userKey, testId, file.getName());
        JSONObject json = this.bzmhc.getJsonForFileUpload(url, file);
        try {
            if (!json.get("response_code").equals(200)) {
                logger.println("Could not upload file " + file.getName() + " " + json.get("error").toString());
            }
        } catch (JSONException e) {
            logger.println("Could not upload file " + file.getName() + " " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @param userKey  - user key
     * @param testId   - test id
     * @param file     - the file (Java class) you like to upload
     * @return test id
     *         //     * @throws java.io.IOException
     *         //     * @throws org.json.JSONException
     */

    public synchronized JSONObject uploadBinaryFile(String userKey, String testId, File file) {

        if (!validate(userKey, testId)) return null;

        String url = this.urlManager.fileUpload(APP_KEY, userKey, testId, file.getName());

        return this.bzmhc.getJsonForFileUpload(url, file);
    }


    public TestInfo getTestRunStatus(String userKey, String testId) {
        TestInfo ti = new TestInfo();

        if (!validate(userKey, testId)) {
            ti.setStatus(TestStatus.NotFound);
            return ti;
        }

        try {
            String url = this.urlManager.testStatus(APP_KEY, userKey, testId);
            JSONObject jo = this.bzmhc.getJson(url, null,BZMHTTPClient.Method.POST);

            if ("Test not found".equals(jo.get("error"))) {
                ti.setStatus(TestStatus.NotFound);
            } else {
                ti.setId(jo.getString("test_id"));
                ti.setName(jo.getString("test_name"));
                ti.setStatus(jo.getString("status"));
            }
        } catch (Exception e) {
            logger.println("error getting status " + e);
            ti.setStatus(TestStatus.Error);
        }
        return ti;
    }

    public synchronized JSONObject startTest(String userKey, String testId) {

        if (!validate(userKey, testId)) return null;

        String url = this.urlManager.testStart(APP_KEY, userKey, testId);
        return this.bzmhc.getJson(url, null, BZMHTTPClient.Method.POST);
    }

    public int getTestCount(String userKey) throws JSONException, IOException, ServletException {
        if (userKey == null || userKey.trim().isEmpty()) {
            logger.println("getTests userKey is empty");
            return 0;
        }

        String url = this.urlManager.getTests(APP_KEY, userKey);

        try {
            JSONObject jo = this.bzmhc.getJson(url, null,BZMHTTPClient.Method.POST);
            if (jo == null){
                return -1;
            }
            else {
                String r = jo.get("response_code").toString();
                if (!r.equals("200")) {
                    return 0;
                }
                JSONArray arr = (JSONArray) jo.get("tests");
                return arr.length();
            }
        } catch (JSONException e) {
            logger.println("Error getting response from server: ");
            e.printStackTrace();
            return -1;
        }
    }


    private boolean validate(String userKey, String testId) {
        if (userKey == null || userKey.trim().isEmpty()) {
            logger.println("startTest userKey is empty");
            return false;
        }

        if (testId == null || testId.trim().isEmpty()) {
            logger.println("testId is empty");
            return false;
        }
        return true;
    }

    /**
     * @param userKey - user key
     * @param testId  - test id
     *                //     * @throws IOException
     *                //     * @throws ClientProtocolException
     */
    public JSONObject stopTest(String userKey, String testId) {
        if (!validate(userKey, testId)) return null;

        String url = this.urlManager.testStop(APP_KEY, userKey, testId);
        return this.bzmhc.getJson(url, null, BZMHTTPClient.Method.POST);
    }

    /**
     * @param userKey  - user key
     * @param reportId - report Id same as Session Id, can be obtained from start stop status.
     *                 //     * @throws IOException
     *                 //     * @throws ClientProtocolException
     */
    public JSONObject aggregateReport(String userKey, String reportId) {
        if (!validate(userKey, reportId)) return null;

        String url = this.urlManager.testAggregateReport(APP_KEY, userKey, reportId);
        return this.bzmhc.getJson(url, null,BZMHTTPClient.Method.POST);

        //####### if failed to getAggregateReport - retry. Move this check to API for V2
/*        for (int i = 0; i < 200; i++) {
            try {
                if (json.get("response_code").equals(404))
                    json = this.api.aggregateReport(apiKey, session);
                else
                    break;
            } catch (JSONException e) {
            } finally {
                Thread.sleep(5 * 1000);
            }
        }*/
        //####### if failed to getAggregateReport - retry. Move this check to API for V2

//################  Make checkAggregate report method to API for V2 ################################
        /*for (int i = 0; i < 30; i++) {
            try {
                if (!json.get("response_code").equals(200)) {
                    logger.println("Error: Requesting aggregate report response code:" + json.get("response_code"));
                } else if (json.has("error") && !json.getString("error").equals("null")){
                    logger.println("Error: Requesting aggregate report. Error received :" + json.getString("error"));
                } else {
                    aggregate = json.getJSONObject("report").get("aggregate").toString();
                }

            } catch (JSONException e) {
                logger.println("Error: Exception while parsing aggregate report [" + e.getMessage() + "]");
            }

            if (!aggregate.equals("null"))
                break;

            Thread.sleep(2 * 1000);
            json = this.api.aggregateReport(apiKey, session);
        }*/
//################  Make checkAggregate report method to API for V2 ################################


    }

    public HashMap<String, String> getTestList(String userKey) throws IOException, MessagingException {

        LinkedHashMap<String, String> testListOrdered = null;

        if (userKey == null || userKey.trim().isEmpty()) {
            logger.println("getTests userKey is empty");
        } else {
            String url = this.urlManager.getTests(APP_KEY, userKey);
            logger.println(url);
            JSONObject jo = this.bzmhc.getJson(url, null,BZMHTTPClient.Method.POST);
            try {
                String r = jo.get("response_code").toString();
                if (r.equals("200")) {
                    JSONArray arr = (JSONArray) jo.get("tests");
                    testListOrdered = new LinkedHashMap<String, String>(arr.length());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject en = null;
                        try {
                            en = arr.getJSONObject(i);
                        } catch (JSONException e) {
                            logger.println("Error with the JSON while populating test list, " + e);
                        }
                        String id;
                        String name;
                        try {
                            if (en != null) {
                                id = en.getString("test_id");
                                name = en.getString("test_name").replaceAll("&", "&amp;");
                                testListOrdered.put(name, id);

                            }
                        } catch (JSONException ie) {
                            logger.println("Error with the JSON while populating test list, " + ie);
                        }
                    }
                }
            }
            catch (Exception e) {
                logger.println("Error while populating test list, " + e);
            }
        }

        return testListOrdered;
    }
}
