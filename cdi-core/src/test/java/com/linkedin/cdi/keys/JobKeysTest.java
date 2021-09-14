// Copyright 2021 LinkedIn Corporation. All rights reserved.
// Licensed under the BSD-2 Clause license.
// See LICENSE in the project root for license information.

package com.linkedin.cdi.keys;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gobblin.configuration.SourceState;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.gobblin.configuration.State;
import com.linkedin.cdi.configuration.MultistageProperties;
import com.linkedin.cdi.util.JsonUtils;
import com.linkedin.cdi.util.ParameterTypes;
import com.linkedin.cdi.util.SchemaBuilder;
import com.linkedin.cdi.util.WorkUnitPartitionTypes;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


@PrepareForTest({JsonUtils.class})
public class JobKeysTest extends PowerMockTestCase {
  private JobKeys jobKeys;
  private Gson gson;

  @BeforeMethod
  public void setUp() {
    jobKeys = new JobKeys();
    gson = new Gson();
  }

  @Test
  public void testIsSessionStateEnabled() {
    JsonObject sessions = new JsonObject();
    jobKeys.setSessionKeyField(sessions);
    Assert.assertFalse(jobKeys.isSessionStateEnabled());

    sessions.addProperty("non-condition", false);
    Assert.assertFalse(jobKeys.isSessionStateEnabled());

    JsonObject nestedObj = new JsonObject();
    sessions.add("condition", nestedObj);
    Assert.assertFalse(jobKeys.isSessionStateEnabled());
    Assert.assertEquals(jobKeys.getSessionStateCondition(), StringUtils.EMPTY);

    nestedObj.addProperty("regexp", "testValue");
    sessions.add("condition", nestedObj);
    Assert.assertTrue(jobKeys.isSessionStateEnabled());
    Assert.assertEquals(jobKeys.getSessionStateCondition(), "testValue");

    JsonObject failConditionNestedObj = new JsonObject();
    sessions.add("failCondition", failConditionNestedObj);
    Assert.assertTrue(jobKeys.isSessionStateEnabled());
    Assert.assertEquals(jobKeys.getSessionStateCondition(), "testValue");
    Assert.assertEquals(jobKeys.getSessionStateFailCondition(), StringUtils.EMPTY);

    failConditionNestedObj.addProperty("regexp", "testFailValue");
    sessions.add("failCondition", failConditionNestedObj);
    Assert.assertTrue(jobKeys.isSessionStateEnabled());
    Assert.assertEquals(jobKeys.getSessionStateCondition(), "testValue");
    Assert.assertEquals(jobKeys.getSessionStateFailCondition(), "testFailValue");
  }

  @Test
  public void testHasSourceSchema() {
    JsonArray sourceSchema = SchemaBuilder.fromJsonData(
        JsonUtils.createAndAddProperty("testKey", "testValue")).buildAltSchema().getAsJsonArray();
    Assert.assertFalse(jobKeys.hasSourceSchema());
    jobKeys.setSourceSchema(sourceSchema);
    Assert.assertTrue(jobKeys.hasSourceSchema());
  }

  @Test
  public void testIsPaginationEnabled() {
    Assert.assertFalse(jobKeys.isPaginationEnabled());

    Map<ParameterTypes, String> paginationFields = new HashMap<>();
    paginationFields.put(ParameterTypes.PAGESIZE, "testValue");
    jobKeys.setPaginationFields(paginationFields);
    Assert.assertTrue(jobKeys.isPaginationEnabled());

    paginationFields = new HashMap<>();
    jobKeys.setPaginationFields(paginationFields);
    Map<ParameterTypes, Long> paginationInitValues = new HashMap<>();
    paginationInitValues.put(ParameterTypes.PAGESIZE, 100L);
    jobKeys.setPaginationInitValues(paginationInitValues);
    Assert.assertTrue(jobKeys.isPaginationEnabled());
  }

  /**
   * Test the validate() method
   *
   * Scenario 1: pagination defined, but no total count field, nor session key field
   *
   * Scenario 2: wrong output schema structure
   */
  @Test
  public void testValidation() {
    // test pagination parameter validation
    SourceState state = new SourceState();
    Map<ParameterTypes, Long> paginationInitValues = new HashMap<>();
    paginationInitValues.put(ParameterTypes.PAGESTART, 0L);
    paginationInitValues.put(ParameterTypes.PAGESIZE, 100L);
    jobKeys.setPaginationInitValues(paginationInitValues);
    Assert.assertTrue(jobKeys.validate(state));

    // test output schema validation with a wrong type
    state.setProp(MultistageProperties.MSTAGE_OUTPUT_SCHEMA.getConfig(), "{}");
    Assert.assertFalse(jobKeys.validate(state));

    // test output schema validation with an empty array
    state.setProp(MultistageProperties.MSTAGE_OUTPUT_SCHEMA.getConfig(), "[{}]");
    Assert.assertFalse(jobKeys.validate(state));

    // test output schema validation with an incorrect structure
    String schema = "[{\"columnName\":\"test\",\"isNullable\":\"true\",\"dataType\":{\"type\":\"string\"}]";
    state.setProp(MultistageProperties.MSTAGE_OUTPUT_SCHEMA.getConfig(), schema);
    jobKeys.initialize(state);
    Assert.assertFalse(jobKeys.validate(state));

    schema = "[{\"columnName\":\"test\",\"isNullable\":\"true\",\"dataType\":{\"type\":\"string\"}}]";
    state.setProp(MultistageProperties.MSTAGE_OUTPUT_SCHEMA.getConfig(), schema);
    jobKeys.setOutputSchema(jobKeys.parseOutputSchema(state));
    Assert.assertTrue(jobKeys.validate(state));

    state.setProp(MultistageProperties.MSTAGE_WORK_UNIT_PARTITION.getConfig(), "lovely");
    jobKeys.setWorkUnitPartitionType(null);
    Assert.assertFalse(jobKeys.validate(state));

    state.setProp(MultistageProperties.MSTAGE_WORK_UNIT_PARTITION.getConfig(), "{\"weekly\": [\"2020-01-01\", \"2020-02-1\"]}");
    jobKeys.setWorkUnitPartitionType(WorkUnitPartitionTypes.COMPOSITE);
    Assert.assertFalse(jobKeys.validate(state));
  }

  @Test
  public void testGetDefaultFieldTypes() throws Exception {
    JobKeys jobkeys = new JobKeys();
    Method method = JobKeys.class.getDeclaredMethod("parseDefaultFieldTypes", State.class);
    method.setAccessible(true);

    State state = Mockito.mock(State.class);
    when(state.getProp(MultistageProperties.MSTAGE_DATA_DEFAULT_TYPE.getConfig(), new JsonObject().toString())).thenReturn("{\"testField\":100}");
    Assert.assertEquals(method.invoke(jobkeys, state).toString(), "{testField=100}");
  }

  @Test
  public void testParseSecondaryInputRetry() throws Exception {
    JobKeys jobkeys = new JobKeys();
    JsonArray input = gson.fromJson("[{\"retry\": {\"threadpool\": 5}}]", JsonArray.class);
    Method method = JobKeys.class.getDeclaredMethod("parseSecondaryInputRetry", JsonArray.class);
    method.setAccessible(true);
    Map<String, Long> actual = (Map) method.invoke(jobkeys, input);
    Assert.assertEquals((long) actual.get("delayInSec"), 300L);
    Assert.assertEquals((long) actual.get("retryCount"), 3);

    input = gson.fromJson("[{\"retry\": {\"delayInSec\": 500,\"retryCount\": 5}}]", JsonArray.class);
    actual = (Map) method.invoke(jobkeys, input);
    Assert.assertEquals((long) actual.get("delayInSec"), 500L);
    Assert.assertEquals((long) actual.get("retryCount"), 5);
  }

  @Test
  public void testGetPaginationInitialValues() throws Exception {
    JobKeys jobkeys = new JobKeys();
    Method method = JobKeys.class.getDeclaredMethod("parsePaginationInitialValues", State.class);
    method.setAccessible(true);

    State state = Mockito.mock(State.class);
    when(state.getProp(MultistageProperties.MSTAGE_PAGINATION.getConfig(), new JsonObject().toString()))
        .thenReturn("{\"fields\": [\"offset\", \"limit\"], \"initialvalues\": [0, 5000]}");
    method.invoke(jobkeys, state);
    Map<ParameterTypes, Long> paginationInitValues = jobkeys.getPaginationInitValues();
    Assert.assertEquals((long) paginationInitValues.get(ParameterTypes.PAGESTART), 0L);
    Assert.assertEquals((long) paginationInitValues.get(ParameterTypes.PAGESIZE), 5000L);
  }

  @Test
  public void testGetPaginationFields() throws Exception {
    JobKeys jobkeys = new JobKeys();
    State state = Mockito.mock(State.class);
    when(state.getProp(MultistageProperties.MSTAGE_PAGINATION.getConfig(), new JsonObject().toString()))
        .thenReturn("{\"fields\": [\"\", \"\"], \"initialvalues\": [0, 5000]}");
    Method method = JobKeys.class.getDeclaredMethod("parsePaginationFields", State.class);
    method.setAccessible(true);
    method.invoke(jobkeys, state);
    Assert.assertEquals(jobkeys.getPaginationInitValues().size(), 0);

    when(state.getProp(MultistageProperties.MSTAGE_PAGINATION.getConfig(), new JsonObject().toString()))
        .thenReturn("{\"initialvalues\": [0, 5000]}");
    method.invoke(jobkeys, state);
    Assert.assertEquals(jobkeys.getPaginationInitValues().size(), 0);
  }
}