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

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Result {
	
	protected static final String RETURN_MESSAGES = "messages";
	protected final String RETURN_RESULT = "result";
	protected final String RETURN_ERRORS = "errors";
	protected final String RESULT_OK = "ok";
	protected final String RESULT_KO = "ko";

	protected JsonObject result;
	
	public Result() {
		result = new JsonObject();
		result.add(RETURN_MESSAGES, new JsonArray());
		result.add(RETURN_ERRORS, new JsonArray());
	}
	
	public Result(String r) {
		this();
		result.addProperty(RETURN_RESULT, r);
	}
	
	public Result(String r, List<String> messages, List<String> errors) {
		this();
		result.addProperty(RETURN_RESULT, r);
		if(messages != null) {
			//result.get(RETURN_MESSAGE).getAsJsonArray().addAll(messages);
		}
		if(errors != null) {
			//result.get(RETURN_ERRORS).getAsJsonArray().addAll(messages);
		}
	}
	
	public void setResult(String r) {
		result.addProperty(RETURN_RESULT, r);
	}
	
	public void setResultOk() {
		setResult(RESULT_OK);
	}
	
	public void setResultKo() {
		setResult(RESULT_KO);
	}
	
	public void addMessage(JsonObject message) {
		result.get(RETURN_MESSAGES).getAsJsonArray().add(message);
	}
	
	public void addMessage(String message) {
		result.get(RETURN_MESSAGES).getAsJsonArray().add(message);
	}
	
	public JsonArray getErrors() {
		return result.get(RETURN_ERRORS).getAsJsonArray();
	}
	
	public void addError(String error) {
		result.get(RETURN_ERRORS).getAsJsonArray().add(error);
	}
	
	public boolean hasErrors() {
		return getErrors().size() > 0;
	}
	
	@Override
	public String toString() {
		return result.toString();
	}
	
	public static String getMessagesProperty() {
		return RETURN_MESSAGES;
	}
}
