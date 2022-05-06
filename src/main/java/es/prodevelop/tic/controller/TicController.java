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

package es.prodevelop.tic.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import es.prodevelop.tic.bo.TicBO;
import es.prodevelop.tic.util.Result;

@RestController
public class TicController {
	
	final static Logger logger = Logger.getLogger(TicController.class);
		
	protected static Gson g = new Gson();
	
	/**
	 * Validates a TIC4.0 message
	 * @param input The json input message to validate
	 * @return A json object in string format:
	 * <br/><b>- result:</b> "ok" / "ko"
	 * <br><b>- errors:</b> a list with the errors
	 * @throws Exception
	 */
	@RequestMapping(value = "/validate", method = RequestMethod.POST)
	@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST})
	public String validate(@RequestBody Object input) throws Exception {
		Result result = new Result();
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> inputAsMap = (HashMap<String, Object>) input;
			JsonObject json = g.fromJson(g.toJson(inputAsMap), JsonObject.class);
			
			result = TicBO.validate(json);
		}
		catch (Exception e) {
			logger.error(toString());
			result.setResultKo();
			result.addError(e.toString());
		}
		return result.toString();
	}
	
	/**
	 * Flatten a TIC4.0 message.
	 * @param entity The complete path to the element to split by. All previous parents will be also split.
	 * @param idField The field to use as id for array objects in the json message. By default is arrayid.
	 * @param input The json input message to flatten
	 * @return A json object in string format:
	 * <br/><b>- result:</b> "ok" / "ko"
	 * <br><b>- messages:</b> a list with the flat messages. Messages are always split by timestamp (and by entity if provided) so a message will be generated for each one.
	 * <br><b>- errors:</b> a list with the errors
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/flatten", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@CrossOrigin(origins = "*", methods= {RequestMethod.GET,RequestMethod.POST})
	public String flatten(
			@RequestParam(required = false, name = "split") String entity, 
			@RequestParam(required = false, name = "subjectidfield") String idField, 
			@RequestBody Object input) throws Exception {
		
		Result result = new Result();
		try {			
			result.setResultOk();
			
			List<JsonObject> messages = new ArrayList<JsonObject>();
			if(input instanceof ArrayList) {
				for(HashMap<String, Object> i : (ArrayList<HashMap<String, Object>>)input) {
					Map<String, Object> inputAsMap = (HashMap<String, Object>) i;
					JsonObject json = g.fromJson(g.toJson(inputAsMap), JsonObject.class);
					messages.addAll(TicBO.getMessages(json, idField, entity));
				}
			}
			else {
				Map<String, Object> inputAsMap = (HashMap<String, Object>) input;
				JsonObject json = g.fromJson(g.toJson(inputAsMap), JsonObject.class);
				messages = TicBO.getMessages(json, idField, entity);				
			}
			
			for(JsonObject m : messages) {
				result.addMessage(m);
			}	
		}
		catch (Exception e) {
			logger.error(toString());
			result.setResultKo();
			result.addError(e.toString());
		}
		return result.toString();
	}
}
