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

package es.prodevelop.tic.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class JsonUtils {
	
	final static Logger logger = Logger.getLogger(JsonUtils.class);
	
	public static Gson g = new Gson();
	
	/**
	 * Transform from a json string to a JsonObject
	 * @param json
	 * @return
	 */
	
	public static JsonObject jsonToJsonObject(String json) {
		return g.fromJson(json, JsonObject.class);
	}
	
	/**
	 * Transform a JsonObject into a String json
	 * @param jsonObject
	 * @return
	 */
	public static String jsonObjectToString(JsonObject jsonObject) {
		return g.toJson(jsonObject);
	}
	
	/**
	 * Split a single json into many depending on the different values of a field
	 * @param source
	 * @param field
	 * @return
	 */
	public static List<JsonObject> splitByKey (JsonObject object, String key) {
		List<JsonObject> messages = new ArrayList<JsonObject>();
		try {
			if(object == null) {
				return messages;
			}
			// Get the distinct values for the key attribute
			Set<String> keyValues = getValuesForKey(object, key);

			// Create a new message for each of the values
			HashMap<String, JsonObject> jsonMap = new HashMap<String, JsonObject>();
			for(String k : keyValues) {
				// Copy all the message
				jsonMap.put(k, object.deepCopy());
			}
			
			// Foreach of the messages
			jsonMap.forEach((k,v) -> {
				// Delete the content that must go in other message
				deleteIfExistAndNotEqual(v, key, k);
				// Clean empty properties
				removeEmptyValues(v);
			});
			
			// Add the final messages
			jsonMap.forEach((k,v) -> {
				messages.add(v);
			});
		}
		catch (Exception e) {
			logger.error("Error: " + e.toString());
		}		
		return messages;
	}
	
	public static List<JsonObject> splitByPath (JsonObject object, String path) {
		List<JsonObject> messages = new ArrayList<JsonObject>();
		try {
			if(object == null) {
				return messages;
			}
			
			if(path == null) {
				messages.add(object);
				return messages;
			}
			
			// First we are going to make the copies of the message, so we need to calculate its number
			getDuplicatesByPath(object, object, "", path, messages);			
		}
		catch (Exception e) {
			logger.error("Error in splitByPath: " + e.toString());
		}		
		return messages;
	}
	
	public static List<JsonObject> getDuplicatesByPath(JsonObject object, JsonObject currentNode, String currentPath, String pathToSplit, List<JsonObject> messages) {
		try {
			String[] steps = pathToSplit.split("[.]");
			
			// If the element is an object
			if(currentNode.get(steps[0]) instanceof JsonObject) {
				// If the path is not completed yet
				if(steps.length > 1) {
					getDuplicatesByPath(object, currentNode.get(steps[0]).getAsJsonObject(), currentPath + steps[0] + ".", pathToSplit.substring(pathToSplit.indexOf(".") + 1), messages);
				}
			}
			// If the element is an array
			else if(currentNode.get(steps[0]) instanceof JsonArray) {
				// Create a document for each of the elements of the array
				for(JsonElement e : currentNode.get(steps[0]).getAsJsonArray()) {
					// Duplicate the object
					JsonObject duplicate = object.deepCopy();
					// Create a new Array with this object
					JsonArray newArray = new JsonArray();
					newArray.add(e);
					// Replace the array with a new with only this element
					replaceElement(duplicate, newArray, currentPath + steps[0]);
					// If there are remaining fields to split by					
					if(steps.length > 1) {
						// Continue splitting
						getDuplicatesByPath(duplicate, e.getAsJsonObject(), currentPath + steps[0] + ".", pathToSplit.substring(pathToSplit.indexOf(".") + 1), messages);
					}
					// If not
					else {
						// Add the message to the list
						messages.add(duplicate);
					}
				}
			}
			else if (currentNode.get(steps[0]) == null) {
				messages.add(object);
			}
			
		}
		catch (Exception e) {
			logger.error("Error in getDuplicatesByPath: " + e.toString());
		}		
		return messages;
	}

	public static void replaceElement(JsonObject currentNode, JsonElement replacement, String pathToReplace) {
		try {
			String[] steps = pathToReplace.split("[.]");
			
			// If it is the last element, we replace
			if(steps.length == 1) {
				currentNode.add(steps[0], replacement);
			}
			// If not, we search the element
			else {
				// If the element is an object
				if(currentNode.get(steps[0]) instanceof JsonObject) {
					replaceElement(currentNode.get(steps[0]).getAsJsonObject(), replacement, pathToReplace.substring(pathToReplace.indexOf(".") + 1));
				}
				// If the element is an array
				else if(currentNode.get(steps[0]) instanceof JsonArray && currentNode.get(steps[0]).getAsJsonArray().size() == 1) {
					replaceElement(currentNode.get(steps[0]).getAsJsonArray().get(0).getAsJsonObject(), replacement, pathToReplace.substring(pathToReplace.indexOf(".") + 1));
				}
			}
		}
		catch (Exception e) {
			logger.error("Error in replaceElement: " + e.toString());
		}		
	}
		
	public static void removeEmptyValues(JsonObject currentNode) {
		try {
			currentNode.keySet().forEach(k -> {
				if(currentNode.get(k) instanceof JsonObject) {
					removeEmptyValues(currentNode.get(k).getAsJsonObject());
	    	    } 
	    	    else if(currentNode.get(k) instanceof JsonArray) {
	    	    	if(currentNode.get(k).getAsJsonArray().isEmpty()) {
	    	    		currentNode.remove(k);
	    	    	}
	    	    	else {
	    	    		currentNode.get(k).getAsJsonArray().forEach(e -> {
		    	    		if(e.isJsonObject()) {
		    	    			removeEmptyValues(e.getAsJsonObject());
		    	    		}
		    	    	});
	    	    	}
		    	}
	    	    else{
		    		if(!has(currentNode, k)) {
		    			currentNode.remove(k);
		    		}
		    	}
		    });
		}
		catch(Exception e) {
			logger.error("Error on removeEmptyValues: " + e.toString());
		}		
	}

	public static void deleteIfExistAndNotEqual(JsonObject currentNode, String key, String value) {
		try {
			currentNode.keySet().forEach(k -> {
				if(currentNode.get(k) instanceof JsonObject) {
					// If the node has the property but with other value, delete from the JsonObject
					if(has(currentNode.get(k).getAsJsonObject(), key) && !value.equals(getAsString(currentNode.get(k).getAsJsonObject(), key))) {
						currentNode.remove(key);
					}
					else {
						deleteIfExistAndNotEqual(currentNode.get(k).getAsJsonObject(), key, value);
					}
	    	    } 
	    	    else if(currentNode.get(k) instanceof JsonArray) {
	    	    	// Call the function for each member of the array
	    	    	JsonArray toRemove = new JsonArray();
	    	    	currentNode.get(k).getAsJsonArray().forEach(e -> {
	    	    		if(e.isJsonObject()) {
	    	    			// If the node has the property but with other value, delete from the JsonObject
	    					if(has(e.getAsJsonObject(), key) && !value.equals(getAsString(e.getAsJsonObject(), key))) {
	    						toRemove.add(e);
	    					}
	    					else {
	    						deleteIfExistAndNotEqual(e.getAsJsonObject(), key, value);
	    					}
	    	    		}
	    	    	});
	    	    	toRemove.forEach(e -> {
	    	    		currentNode.get(k).getAsJsonArray().remove(e);
	    	    	});
		    	}	
		    });
		}
		catch(Exception e) {
			logger.error("Error on deleteIfExistAndNotEqual: " + e.toString());
		}		
	}

	public static Set<String> getValuesForKey(JsonObject object, String key) {
		Set<String> values = new HashSet<String>();
		try {
			if(key == null) {
				return values;
			}
			getValuesForKey(object, key, values);		
		}
		catch (Exception e) {
			logger.error("Error: " + e.toString());
		}		
		return values;
	}
	
	public static void getValuesForKey(JsonObject currentNode, String key, Set<String> values) {				
		try {
			currentNode.keySet().forEach(k -> {
				if(currentNode.get(k) instanceof JsonObject) {
					getValuesForKey(currentNode.get(k).getAsJsonObject(), key, values);
	    	    } 
	    	    else if(currentNode.get(k) instanceof JsonArray) {
	    	    	currentNode.get(k).getAsJsonArray().forEach(e -> {
	    	    		if(e.isJsonObject()) {
	    	    			getValuesForKey(e.getAsJsonObject(), key, values);
	    	    		}
	    	    	});
		    	}
		    	else{
		    		if(k.equals(key)) {
		    			values.add(currentNode.get(key).getAsString());
		    		}
		    	}	
		    });
		}
		catch(Exception e) {
			logger.error("Error: " + e.toString());
		}
	}
	
	/**
	 * Transforms a hierarchical JSON in an object with just one level
	 * @param object
	 */
	public static JsonObject flatten(JsonObject object) {		
		if(object == null) {
			return null;
		}		
		JsonObject flattenedObject = new JsonObject();
		
		String currentPath = "";
		
		flatten(flattenedObject, object, currentPath);
		
		return flattenedObject;
	}
	
	/**
	 * Transforms a hierarchical JSON in an object with just one level
	 * @param flattened
	 * @param currentNode
	 * @param path
	 */
	public static void flatten(JsonObject flattened, JsonObject currentNode, String path) {				
		currentNode.keySet().forEach(key -> {
			if(currentNode.get(key) instanceof JsonObject) {
        		flatten(flattened, currentNode.get(key).getAsJsonObject(), path + key + ".");
    	    } 
    	    else if(currentNode.get(key) instanceof JsonArray) {
    	    	flattened.add(path + key, currentNode.get(key));
	    	}
	    	else{
	    		flattened.add(path + key, currentNode.get(key));
	    	}	
	    });
	}
	
	public static boolean has(JsonObject jsonObject, String property) {
		if(jsonObject != null && jsonObject.has(property) && jsonObject.get(property) != null && !(jsonObject.get(property) instanceof JsonNull)) {
			return true;
		}
		return false;
	}
	
	public static String getAsString(JsonObject jsonObject, String property) {
		if(has(jsonObject, property)) {
			return jsonObject.get(property).getAsString();
		}
		return null;
	}

}
