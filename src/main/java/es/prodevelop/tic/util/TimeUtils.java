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

import java.time.Instant;

import org.apache.log4j.Logger;

public class TimeUtils {
	
	final static Logger logger = Logger.getLogger(TimeUtils.class);
	
	public static String getCurrentTimestamp() {
		return getTimestampFromEpoch(System.currentTimeMillis() / 1000);
	}
	
	public static long getCurrentEpoch() {
		return System.currentTimeMillis() / 1000;
	}
	
	public static String getTimestampFromEpoch(long epoch) {
		return Instant.ofEpochSecond(epoch).toString();
	}

	public static long getEpochFromTimestamp(String timestamp) {	
		long epoch = 0;
		
		if (timestamp == null || timestamp.isEmpty()) {
			return 0;
		}

		timestamp = timestamp.replace(".000000000", "");
		if(timestamp.contains("AM")) {
			timestamp.replace("AM", "");
		} 
		else if (timestamp.contains("PM")) {
			timestamp.replace("PM", "");
		}
		
		try {
			epoch = Instant.parse(timestamp).getEpochSecond();
		}
		catch (Exception e) {
			logger.debug("Error on getEpochOnSecondsFromTimestamp: " + timestamp + ". " + e.toString());
		}
		return epoch;
	}
		
	public static String addSeconds(String timestamp, long seconds) {	
		long epoch = getEpochFromTimestamp(timestamp);
		return getTimestampFromEpoch(epoch + seconds);		
	}
	
	public static String addMinutes(String timestamp, long minutes) {	
		return addSeconds(timestamp, minutes * 60);		
	}
	
	public static String addHours(String timestamp, long hours) {	
		return addMinutes(timestamp, hours * 60);		
	}
	
	public static String addDays(String timestamp, long days) {	
		return addHours(timestamp, days * 24);		
	}
	
	public static boolean before(String timestamp1, String timestamp2) {
		return (getEpochFromTimestamp(timestamp1) < getEpochFromTimestamp(timestamp2));
	}
	
	public static boolean beforeOrEqual(String timestamp1, String timestamp2) {
		return (getEpochFromTimestamp(timestamp1) <= getEpochFromTimestamp(timestamp2));
	}
	
	public static boolean sameTime(String timestamp1, String timestamp2) {
		return (getEpochFromTimestamp(timestamp1) == getEpochFromTimestamp(timestamp2));
	}
	
	public static boolean afterOrEqual(String timestamp1, String timestamp2) {
		return (getEpochFromTimestamp(timestamp1) >= getEpochFromTimestamp(timestamp2));
	}
	
	public static boolean after(String timestamp1, String timestamp2) {
		return (getEpochFromTimestamp(timestamp1) > getEpochFromTimestamp(timestamp2));
	}
	
	public static boolean between(String start, String end, String timestamp) {
		return afterOrEqual(timestamp, start) && beforeOrEqual(timestamp, end);
	}
	
	public static long getSeconds(String timestamp1, String timestamp2) {
		long epoch1 = TimeUtils.getEpochFromTimestamp(timestamp1);
		long epoch2 = TimeUtils.getEpochFromTimestamp(timestamp2);
		
		return Math.abs(epoch2 - epoch1);
	}
}
