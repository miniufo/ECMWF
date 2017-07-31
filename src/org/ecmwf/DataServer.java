/*
 *
 * (C) Copyright 2012-2013 ECMWF.
 *
 * This software is licensed under the terms of the Apache Licence Version 2.0
 * which can be obtained at http://www.apache.org/licenses/LICENSE-2.0. 
 * In applying this licence, ECMWF does not waive the privileges and immunities 
 * granted to it by virtue of its status as an intergovernmental organisation nor
 * does it submit to any jurisdiction.
 *
 */

package org.ecmwf;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;


public class DataServer extends Retriever {

	public void retrieve(JSONObject req) throws IOException, APIError {
		String dataset = req.getString("dataset");
		String target  = req.getString("target");
		retrieve("/datasets/" + dataset, req, target);
	}

	public void retrieve(Map<String,String> req) throws IOException, APIError {
		JSONObject s = new JSONObject();
		Iterator<Entry<String, String>> it = req.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> pairs = it.next();
			s.put(pairs.getKey(),pairs.getValue());
		}
		retrieve(s);
	}
}
