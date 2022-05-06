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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ErrorUtils {
	
	private static final String ERRORS_PROPERTIES_FILE = "errors.properties";
	private static HashMap<String, Object> errors = load();   
    
    public static HashMap<String, Object> load(){
        HashMap<String, Object> prop = new HashMap<String, Object>();
                
        try {
            java.util.Properties loadedPropertiesFile = new java.util.Properties();
            InputStream in = ErrorUtils.class.getClassLoader().getResourceAsStream(ERRORS_PROPERTIES_FILE);
            loadedPropertiesFile.load(in);
            in.close();
            
            for (String key : loadedPropertiesFile.stringPropertyNames()) {
            	prop.put(key, loadedPropertiesFile.getProperty(key));
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        
        // Overwrite with system environment variables
        try {
        	Map<String, String> map = System.getenv();
            for (String key : map.keySet()) {                
                prop.put(key, map.get(key));
            }
        }
        catch(Exception e) {
        	e.printStackTrace();
        }
        
        return prop;
    }
    
    public static String get(String key) {
    	return (String) errors.get(key);
    }
    
    public static String get(String key, String arg0) {
    	String message = (String) errors.get(key);
    	return message != null ? message.replace("{0}", arg0) : message;
    }
    
    public static String get(String key, String arg0, String arg1) {
    	String message = (String) errors.get(key);
    	return message != null ? message.replace("{0}", arg0).replace("{1}", arg1) : message;
    }

    public static void reload() {
    	errors = load();
    }
        
    public static boolean getAsBoolean(String key) {
    	return (errors.get(key) != null && Long.parseLong((String) errors.get(key)) > 0) ? true : false;
    }
    
    public static Long getAsLong(String key) {
    	return errors.get(key) != null ? Long.parseLong((String) errors.get(key)) : null;
    }
    
    public static Integer getAsInteger(String key) {
    	return errors.get(key) != null ? Integer.parseInt((String) errors.get(key)) : null;
    }
}
