/*
* Licensed to Prodevelop SL under one
* or more contributor license agreements.  
* Prodevelop SL licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
* 
* For more information, contact:
*
*   Prodevelop, S.L.
*   C/ Cronista Carreres, 13 – entlo 2-4
*   46003 Valencia
*   Spain
*
*   +34 963 510 612
* 
*   prode@prodevelop.es
*   https://www.prodevelop.es
* 
* @author Héctor Iturria Sánchez 
*/

package es.prodevelop.tic.bo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import es.prodevelop.tic.util.Configuration;
import es.prodevelop.tic.util.ErrorUtils;
import es.prodevelop.tic.util.JsonSchemaUtils;
import es.prodevelop.tic.util.JsonUtils;
import es.prodevelop.tic.util.Result;
import es.prodevelop.tic.util.TimeUtils;
import es.prodevelop.tic.util.ValidationUtils;

public class TicBO {
	
	final static Logger logger = Logger.getLogger(TicBO.class);
	
	static List<String> fieldsToPath = Arrays.asList(Configuration.get("FIELD_TO_PATH").split(","));
	static List<String> closeValueKeyFields = Arrays.asList(Configuration.get("CLOSE_VALUE_KEYFIELDS").split(","));
	static List<String> openValueKeyFields = Arrays.asList(Configuration.get("OPEN_VALUE_KEYFIELDS").split(","));
	static List<String> validationKeyFields = Arrays.asList(Configuration.get("VALIDATION_KEY_FIELDS").split(","));
	static HashMap<String, Boolean> fieldsToPathMap = buildMapFromList(fieldsToPath);
	static HashMap<String, String> pathToFieldMap = buildPathToFieldMap();
	static HashMap<String, Boolean> closeValueKeyFieldsMap = buildMapFromList(closeValueKeyFields);
	static HashMap<String, Boolean> openValueKeyFieldsMap = buildMapFromList(openValueKeyFields);
	static HashMap<String, Boolean> validationKeyFieldsMap = buildMapFromList(validationKeyFields);
		
	public static List<JsonObject> getMessages(JsonObject object, String idField, String path) throws Exception{ 
		List<JsonObject> messages = new ArrayList<JsonObject>();		
		try {
			// Get the message properties
			JsonObject messageProperties = getMessageProperties(object);
					
			// Messages are split 
			List<JsonObject> messagesByTimestamp = JsonUtils.splitByKey(object, "timestamp");
			
			// Once messages are split by timestamp, we split then again if there is any other field
			List<JsonObject> messagesByArray = new ArrayList<JsonObject>();
			if(path != null) {
				for(JsonObject message : messagesByTimestamp) {
					messagesByArray.addAll(JsonUtils.splitByPath(message, path));
				}
			}
			else {
				messagesByArray = messagesByTimestamp;
			}
			
			String finalIdField = (idField != null ? idField : Configuration.get("FIELD_ID"));
			
			// For each one of the split messages
			for(int i = 0; i < messagesByArray.size(); i++) {
				// Copy the header for each message
				if(messageProperties != null) {
					addMessageProperties(messagesByArray.get(i), messageProperties, i + 1);			
				}
				// Flatten
				JsonObject flatMessage = flatten(messagesByArray.get(i), finalIdField);
				// Add the message to the return list
				messages.add(flatMessage);
			}			
		}
		catch(Exception e) {
			logger.error("Error in getMessages: " + e.toString());
		}
		
		return messages;
	}
	
	/**
	 * Creates a map from a list of values
	 * @return
	 */	
	private static HashMap<String, Boolean> buildMapFromList(List<String> values) {		
		HashMap<String, Boolean> fieldsToMap = new HashMap<String, Boolean>();
		for(String field : values) {
			fieldsToMap.put(field, true);
		}
		return fieldsToMap;
	}
	
	private static HashMap<String, String> buildPathToFieldMap() {		
		HashMap<String, String> fieldsToMap = new HashMap<String, String>();
		for(String field : fieldsToPath) {
			String valuesInline = ValidationUtils.get(field);
			if(valuesInline != null) {
				String[] values = valuesInline.split("[|]");
				for(String value : values) {
					if(value.length() > 0) {
						fieldsToMap.put(value, field);
					}
				}
			}
		}
		return fieldsToMap;
	}

	public static JsonObject flatten(JsonObject object, String idField) {
		JsonObject flat = new JsonObject();
		try {
			if(object == null) {
				return null;
			}		
			
			String currentPath = "";			
			
			flatten(flat, object, idField, currentPath);
			
			return flat;
		}
		catch(Exception e) {
			logger.error("Error in flatten: " + e.toString());
		}		
		return flat;
	}
	
	public static void flatten(JsonObject flat, JsonObject currentNode, String idField, String path) {				
		// Build the current path depending on the fields of the object (if it is an object)
		String pathFromFieldValues = "";
		boolean hasTimestamp = false;
		if(currentNode instanceof JsonObject) {
			pathFromFieldValues = buildPathWithFields(currentNode);
			hasTimestamp = JsonUtils.has(currentNode, "timestamp");
		}
		
		for(String key : currentNode.keySet()) {
			// Object
			if(currentNode.get(key) instanceof JsonObject) {
				flatten(flat, currentNode.get(key).getAsJsonObject(), idField, path + pathFromFieldValues + key + ".");
    	    } 
			// Array
    	    else if(currentNode.get(key) instanceof JsonArray) {
    	    	/* Commented because all arrays must add the id, also if there is just 1 child
    	    	// If there is only one element in the array
    	    	if(currentNode.get(key).getAsJsonArray().size() == 1) {
    	    		flatten(flat, currentNode.get(key).getAsJsonArray().get(0).getAsJsonObject(), idField, path + key + "." + pathFromFieldValues);
    	    	}
    	    	// If there is more than one
    	    	else {
    	    	*/
    	    	boolean isOneElementArray = (currentNode.get(key).getAsJsonArray().size() == 1);
	    		for(JsonElement e : currentNode.get(key).getAsJsonArray()) {
    	    		// If the element of the array is an object, flat it
    	    		if(e.isJsonObject()) {
    	    			String finalPath = pathFromFieldValues;
    	    			if(!isOneElementArray && JsonUtils.has(e.getAsJsonObject(), idField)) {
    	    				finalPath = Configuration.get("MARKER_ID") + JsonUtils.getAsString(e.getAsJsonObject(), idField) + "." + pathFromFieldValues;
    	    			}
    	    			else if(buildPathWithFields(e.getAsJsonObject()).length() == 0) {
    	    				finalPath = Configuration.get("MARKER_ID") + "." + pathFromFieldValues;
    	    			}
    	    			flatten(flat, e.getAsJsonObject(), idField, path + key + "." + finalPath);
    	    		}
    	    		// If the array is built from values, keep them
    	    		else {
    	    			if(fieldsToPathMap.get(key) == null) {
    		    			flat.add(path + pathFromFieldValues + key, currentNode.get(key));
    		    		}
    	    		}
    	    	}
    	    	//}    	    	
	    	}
			// Property
	    	else{
	    		// Add only if the value is not added to the path
	    		if(!hasTimestamp || fieldsToPathMap.get(key) == null) {
	    			flat.add(path + pathFromFieldValues + key, currentNode.get(key));
	    		}
	    	}
		}
	}
	
	/**
	 * Builds a path getting values from the special fields
	 * @return
	 */
	public static String buildPathWithFields(JsonObject currentNode) {
		StringBuilder sb = new StringBuilder("");
		// Only observations can have key fields. Timestamp is mandatory
		if(JsonUtils.has(currentNode, "timestamp")) {
			for(String field : fieldsToPath) {
				if(JsonUtils.has(currentNode, field)) {
					String prefix = "";
					if(openValueKeyFieldsMap.get(field) != null) {
						prefix = Configuration.get("MARKER_KEYFIELD") + field + Configuration.get("MARKER_KEYFIELD");
					}
					sb.append(prefix + currentNode.get(field).getAsString().toLowerCase()).append("."); 
				}
			}
		}		
		return sb.toString();
	}

	public static JsonObject getMessageProperties(JsonObject object) {
		JsonObject metadata = JsonUtils.has(object, "msg")? object.get("msg").getAsJsonObject() : null;
		//object.remove("msg");
		return metadata;
	}
	
	public static void addMessageProperties(JsonObject object, JsonObject metadata, int sampleId) {
		if(!JsonUtils.has(object, "msg")) {
			object.add("msg", metadata.deepCopy());
		}
		object.get("msg").getAsJsonObject().addProperty("sample", sampleId);
	}
	
	public static boolean hasTimestamp(JsonObject object) {
		return JsonUtils.has(object, "timestamp");
	}
	
	public static String getTimestamp(JsonObject object) {
		return JsonUtils.getAsString(object, "timestamp");
	}

	public static boolean hasTimestampRange(JsonObject object) {
		return JsonUtils.has(object, "starttimestamp") && JsonUtils.has(object, "endtimestamp") ;
	}
	
	public static boolean isInRange(JsonObject object, String timestamp) {
		if(hasTimestampRange(object)) {
			return TimeUtils.between(JsonUtils.getAsString(object, "starttimestamp"), JsonUtils.getAsString(object, "endtimestamp"), timestamp);
		}
		else {
			return false;
		}
	}
	
	/**
	 * Returns if a message has a valid Tic4.0 structure
	 * What to validate:
	 * 	1. Objects need to have a timestamp or fields added to the path
	 * @param object
	 * @return
	 */
	public static Result validate(JsonObject object) {
		Result result = new Result();
		try {
			if(object == null) {
				return null;
			}
			
			// Json schema validation
			JsonSchema schema = JsonSchemaUtils.getJsonSchemaFromClasspath("schema.json");
			
			ObjectMapper mapper = new ObjectMapper();
		    JsonNode jsonNode = mapper.readTree(JsonUtils.jsonObjectToString(object));			
			Set<ValidationMessage> errors = schema.validate(jsonNode);
			for(ValidationMessage vm : errors) {
				result.addError(vm.getMessage().substring(2));
			}
			
			// Custom validations
			String currentPath = "";
			validate(result, object, currentPath);
			
			// Set the result status
			if(result.hasErrors()) {
				result.setResultKo();
			}
			else {
				result.setResultOk();
				result.addMessage("The JSON file is compliant with TIC 4.0");
			}
		}
		catch(Exception e) {
			logger.error("Error in validate: " + e.toString());
			result.setResultKo();
			result.addError(e.toString());
		}
		return result;
	}
	
	public static void validate(Result result, JsonObject currentNode, String path) {				
		
		for(String key : currentNode.keySet()) {
			// Object
			if(currentNode.get(key) instanceof JsonObject) {
				// Objects do not need a validation, propagate to children
				validate(result, currentNode.get(key).getAsJsonObject(), path + key + ".");
    	    } 
			// Array
    	    else if(currentNode.get(key) instanceof JsonArray) {
    	    	// Arrays must have elements that have a different timestamp value or at least special fields that do not repeat themselves
    	    	HashMap<String, List<JsonObject>> arrayElementsWithKeyMap = new HashMap<String, List<JsonObject>>();
    	    	int i = 0;
    	    	for(JsonElement e : currentNode.get(key).getAsJsonArray()) {
    	    		// If the element of the array is an object, build an unique identifier from its properties
    	    		if(e.isJsonObject()) {
    	    			// Build a unique identifier using the object values of special fields
    	    			StringBuilder sb = new StringBuilder("key_");
	    				for(String field : validationKeyFields) {
	    					if(JsonUtils.has(e.getAsJsonObject(), field)) {
	    						sb.append(e.getAsJsonObject().get(field).getAsString().toLowerCase()).append("."); 
	    					}
	    				}
	    				// Remove last dot
	    				if(sb.length() > 4) {
	    					sb.deleteCharAt(sb.length() - 1);
	    				}
	    					    				
	    				// Add the object to the map
	    				if(arrayElementsWithKeyMap.get(sb.toString()) == null) {
	    					arrayElementsWithKeyMap.put(sb.toString(), new ArrayList<JsonObject>());
	    				}
	    				arrayElementsWithKeyMap.get(sb.toString()).add(e.getAsJsonObject());
    	    				    				
	    				// Validate children
    	    			validate(result, e.getAsJsonObject(), path + key + "[" + i + "].");
    	    		}
    	    		i++;
    	    	}
    	    	
    	    	// Keep track of elements that require a timestamp
    	    	List<JsonObject> requireTimestamp = new ArrayList<JsonObject>();
    	    	
    	    	// Once we have a map with key values and the JsonObject we need to validate that lists with more than one item have different timestamps
				for(Entry<String, List<JsonObject>> entry : arrayElementsWithKeyMap.entrySet()) {
					if(entry.getValue().size() > 1) {
						// Check if the timestamps are different (and there is a timestamp)
						HashMap<String, List<JsonObject>> elementsByTimestampMap = new HashMap<String, List<JsonObject>>();
						for(JsonObject object : entry.getValue()) {							
							// If there is timestamp
							if(JsonUtils.has(object, "timestamp")) {
								// Add to the map
								if(elementsByTimestampMap.get(JsonUtils.getAsString(object, "timestamp")) == null) {
									elementsByTimestampMap.put(JsonUtils.getAsString(object, "timestamp"), new ArrayList<JsonObject>());
								}
								elementsByTimestampMap.get(JsonUtils.getAsString(object, "timestamp")).add(object);
							}
							// If no timestamp
							else {
								requireTimestamp.add(object);
							}
						}
						// Check if there are repeated timestamps
						for(Entry<String, List<JsonObject>> entryByTimestamp : elementsByTimestampMap.entrySet()) {
							if(entryByTimestamp.getValue().size() > 1) {
								result.addError(ErrorUtils.get("ERROR_PROPERTY_DUPLICATED", path + key));
							}
						}
					}
				}
				
				// Elements that appear more than once and have no timestamp
				i = 0;
    	    	for(JsonElement e : currentNode.get(key).getAsJsonArray()) {
    	    		// If the element of the array is an object
    	    		if(e.isJsonObject()) {
    	    			for(JsonObject o : requireTimestamp) {
    	    				if(o.equals(e)) {
    	    					result.addError(ErrorUtils.get("ERROR_NO_TIMESTAMP", path + key+ "[" + i + "]"));
    	    				}
    	    			}
    	    		}
    	    		i++;
    	    	}
				
	    	}
			// Property
	    	else{
	    		if(!ValidationUtils.validate(key, currentNode.get(key).getAsJsonPrimitive())) {
	    			result.addError(ErrorUtils.get("ERROR_PROPERTY_NOT_ALLOWED_VALUE", path + key, JsonUtils.getAsString(currentNode, key)));
	    		}
	    	}
		}
	}
	
	/**
	 * Build a TIC message from a flat json
	 * @param object
	 * @return
	 * @throws Exception
	 */
	public static List<JsonObject> buildMessages(JsonObject object, String idField) throws Exception{ 
		List<JsonObject> messages = new ArrayList<JsonObject>();		
		
		if(object == null) return messages;
		
		// Messages to build
		List<JsonObject> sourceMessages = new ArrayList<JsonObject>();
		
		try {			
			if(object.isJsonObject()) {
				// If it is an array of messages from a flatten result
				if(JsonUtils.has(object, Result.getMessagesProperty()) && object.get(Result.getMessagesProperty()).isJsonArray()) {
					for(JsonElement e : object.get(Result.getMessagesProperty()).getAsJsonArray()) {
						if(e.isJsonObject()) {
							sourceMessages.add(e.getAsJsonObject());
						}
					}
				}
				// If it is a single message
				else {
					sourceMessages.add(object);
				}
			}			
			// If there is an array of messages
			else if(object.isJsonArray()) {
				for(JsonElement e : object.getAsJsonArray()) {
					if(e.isJsonObject()) {
						sourceMessages.add(e.getAsJsonObject());
					}
				}
			}
			
			String finalIdField = (idField != null ? idField : Configuration.get("FIELD_ID"));
			
			// Build each of the messages
			for(JsonObject o : sourceMessages) {
				messages.add(buildMessage(o, finalIdField));
			}			
		}
		catch(Exception e) {
			logger.error("Error in buildMessages: " + e.toString());
		}		
		return messages;
	}
	
	public static JsonObject buildMessage(JsonObject object, String idField) throws Exception{ 
		
		// Message to build
		JsonObject treeObject = new JsonObject();	
		
		try {			
			object.keySet().forEach(key -> {
				String[] path = key.split("[.]");
				JsonElement parent = treeObject;
				JsonElement grandparent = treeObject;
				
				HashMap<String, String> keyFieldMap = new HashMap<String, String>();
				
				for(int i = 0; i < path.length; i++) {
					// If it is an id path
					if(path[i].startsWith(Configuration.get("MARKER_ID"))) {
						// If the parent is an object, change it to Array
						if(parent.isJsonObject()) {
							parent = new JsonArray();
							grandparent.getAsJsonObject().add(path[i-1], parent);
						}
						// Look for object or create if it does not exist
						boolean found = false;
						for(JsonElement e : parent.getAsJsonArray()) {
							if(path[i].length() == 1
									|| path[i].substring(1).equals(JsonUtils.getAsString(e.getAsJsonObject(), idField))
									|| !JsonUtils.has(e.getAsJsonObject(), idField)) {
								// If we find the object it is already created, just select as current parent
								grandparent = parent;
								parent = e.getAsJsonObject();
								found = true;
							}
						}
						if(!found) {
							JsonObject o = new JsonObject();
							grandparent = parent;
							parent.getAsJsonArray().add(o);
							parent = o;
						}
					}
					// If it as a value that was added into the path (pom/pomt)
					else if(pathToFieldMap.get(path[i]) != null) {
						// If the parent is an object, change it to Array
						if(parent.isJsonObject()) {
							parent = new JsonArray();
							grandparent.getAsJsonObject().add(path[i-1], parent);
						}
						keyFieldMap.put(pathToFieldMap.get(path[i]), path[i]);
					}
					// If it as a value that was added into the path
					else if(path[i].startsWith(Configuration.get("MARKER_UNIT"))) {
						// If the parent is an object, change it to Array
						if(parent.isJsonObject()) {
							parent = new JsonArray();
							grandparent.getAsJsonObject().add(path[i-1], parent);
						}
						keyFieldMap.put("unit", path[i].substring(1));
					}
					// If it as a value that was added into the path
					else if(path[i].startsWith(Configuration.get("MARKER_REFERENCE"))) {
						// If the parent is an object, change it to Array
						if(parent.isJsonObject()) {
							parent = new JsonArray();
							grandparent.getAsJsonObject().add(path[i-1], parent);
						}
						keyFieldMap.put("reference", path[i].substring(1));
					}
					// Add path fields to the object when having a value 
					else if(keyFieldMap.size() > 0) {
						boolean found = false;
						for(JsonElement e : parent.getAsJsonArray()) {
							boolean match = true;
							for(String k : keyFieldMap.keySet()) {
								if(!keyFieldMap.get(k).equals(JsonUtils.getAsString(e.getAsJsonObject(), k))){
									match = false;
									break;
								}
							}
							if(match) {
								found = true;
								e.getAsJsonObject().add(path[i], object.get(key));
								grandparent = parent;
								parent = e.getAsJsonObject();
							}
						}
						if(!found) {
							JsonObject o = new JsonObject();
							for(String k : keyFieldMap.keySet()) {
								o.addProperty(k, keyFieldMap.get(k));
							}
							o.add(path[i], object.get(key));
							grandparent = parent;
							parent.getAsJsonArray().add(o);
							parent = o;
						}						
					}
					// Just parent and child
					else {
						if(parent.getAsJsonObject().get(path[i]) == null) {
							parent.getAsJsonObject().add(path[i], i < path.length - 1 ? new JsonObject() : object.get(key));
						}
						grandparent = parent; 
						parent = parent.getAsJsonObject().get(path[i]);
					}					
				}			
		    });		
		}
		catch(Exception e) {
			logger.error("Error in buildMessage: " + e.toString());
		}		
		return treeObject;
	}
}
